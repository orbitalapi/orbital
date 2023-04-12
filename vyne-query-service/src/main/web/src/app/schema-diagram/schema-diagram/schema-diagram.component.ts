import { ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { Schema, SchemaMember, SchemaMemberKind, splitOperationQualifiedName } from '../../services/schema';
import { RequiredMembersProps, SchemaFlowWrapper, SchemaMemberClickProps } from './schema-flow.react';
import { ResizedEvent } from 'angular-resize-event/lib/resized.event';
import { isNullOrUndefined } from 'util';
import { Observable, Subject } from 'rxjs';
import { map } from 'rxjs/operators';
import { arraysEqual } from 'src/app/utils/arrays';
import { LinkKind } from 'src/app/schema-diagram/schema-diagram/schema-chart-builder';
import { Router } from '@angular/router';

@Component({
  selector: 'app-schema-diagram',
  styleUrls: ['./schema-diagram.component.scss'],
  template: `
    <!-- we need a wrapper to catch the resize events, and then
    provide explicit sizing to container -->
    <h3 *ngIf="title">{{ title }}</h3>
    <div class="wrapper" (resized)="onWrapperResized($event)">
      <div #container></div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SchemaDiagramComponent {

  constructor(private router: Router) {
  }

  private _visibleLinkKinds: LinkKind[] = ['entity'];
  @Input()
  get visibleLinkKinds(): LinkKind[] {
    return this._visibleLinkKinds;
  }

  set visibleLinkKinds(value: LinkKind[]) {
    if (arraysEqual(this._visibleLinkKinds, value)) {
      return;
    }
    this._visibleLinkKinds = value;
    this.resetComponent();
  }

  private _displayedMembers: string[] | 'everything' | 'services' = 'services';



  @Input()
  title: string;

  @Input()
  get displayedMembers(): string[] | 'everything' | 'services' {
    return this._displayedMembers;
  }

  set displayedMembers(value: string[] | 'everything' | 'services') {
    if (this._displayedMembers === value) {
      return
    }
    if (Array.isArray(value) && Array.isArray(this._displayedMembers) && arraysEqual(value, this._displayedMembers)) {
      return
    }
    this._displayedMembers = value;
    // When the displayed members reference has changed, destroy and rebuild the whole thing
    this.resetComponent();
    this.updateComponent();
  }

  private _schema$: Observable<Schema>;
  @Input()
  get schema$(): Observable<Schema> {
    return this._schema$;
  }

  @Output()
  fullscreenChange = new EventEmitter();

  set schema$(value) {
    if (this._schema$ === value) {
      return;
    }
    this._schema$ = value;
    this.updateComponent();
  }

  private lastMeasureEvent: ResizedEvent | null = null;

  onWrapperResized(event: ResizedEvent) {
    if (!this.isSignificantResize(event)) {
      return;
    }

    this.lastMeasureEvent = event;

    this.containerRef.nativeElement.width = event.newRect.width;
    this.containerRef.nativeElement.height = event.newRect.height;

    this.updateComponent();
  }

  // Added this, as was getting lots of resize events from flex layout algos when the
  // size was changing by a few pixels
  private isSignificantResize(event: ResizedEvent) {
    if (!this.lastMeasureEvent) {
      return true;
    }
    if (isNullOrUndefined(event.newRect.height) || isNullOrUndefined(event.newRect.width)) {
      return false;
    }
    return (Math.abs(event.newRect.height - this.lastMeasureEvent.newRect.height) > 10 ||
      Math.abs(event.newRect.width - this.lastMeasureEvent.newRect.width) > 10);
  }

  private _containerRef: ElementRef;

  @ViewChild('container')
  get containerRef(): ElementRef {
    return this._containerRef;
  }

  set containerRef(value: ElementRef) {
    if (this._containerRef === value) {
      return;
    }
    this._containerRef = value;
    this.updateComponent();
  }

  resetComponent() {
    console.log('Destroying and rebuilding schema diagram')
    if (this.containerRef && this.containerRef.nativeElement)  {
      SchemaFlowWrapper.destroy(this.containerRef);
    }
  }

  private updateComponent() {
    if (!this.schema$ || !this.displayedMembers || !this.containerRef || !this.lastMeasureEvent) {
      return;
    }

    const clickHandler = new Subject<SchemaMemberClickProps>();
    clickHandler.subscribe((clickedMember:SchemaMemberClickProps) => {
      const navigate = (schemaMemberType:SchemaMemberKind, name: string) => {
        switch (schemaMemberType) {
          case 'TYPE':
            this.router.navigate(['catalog',name]);
            break;
          case 'OPERATION':
            const operationName = splitOperationQualifiedName(name);
            this.router.navigate(['services',operationName.serviceName, operationName.operationName])
            break;
          case 'SERVICE':
            this.router.navigate(['services',name])
            break;
        }
      }

      if ("type" in clickedMember) {
        navigate(clickedMember.type, clickedMember.name.memberQualifiedName.fullyQualifiedName);
      } else {
        navigate(clickedMember.kind, clickedMember.name.fullyQualifiedName);
      }
    })

    const membersToDisplay: Observable<RequiredMembersProps> = this.schema$.pipe(
      map(schema => {
        if (this.displayedMembers === 'everything') {
          const membersToDisplay = schema.types
            .filter(t => !t.name.namespace.startsWith('lang.taxi') && !t.name.namespace.startsWith('io.vyne') && !t.isScalar)
            .map(t => t.name.parameterizedName)
            .concat(schema.services.map(s => s.qualifiedName));
          return {
            schema,
            memberNames: membersToDisplay
          }
        } else if (this.displayedMembers === 'services') {
          return {
            schema,
            memberNames: schema.services.map(s => s.qualifiedName)
          }
        } else {
          return { schema, memberNames: this.displayedMembers }
        }
      }));

    SchemaFlowWrapper.initialize(
      this.containerRef,
      membersToDisplay,
      this.schema$,
      this.lastMeasureEvent.newRect.width,
      this.lastMeasureEvent.newRect.height,
      this.visibleLinkKinds,
      clickHandler
    )
  }
}

