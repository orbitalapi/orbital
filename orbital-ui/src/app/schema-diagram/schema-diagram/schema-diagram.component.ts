import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  EventEmitter,
  Input, OnInit,
  Output,
  ViewChild
} from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, Subject } from 'rxjs';
import { map } from 'rxjs/operators';
import { Schema, SchemaMemberKind, splitOperationQualifiedName, Type } from '../../services/schema';
import { SchemaAndRequiredMembersProps, SchemaFlowWrapper, SchemaMemberClickProps } from './schema-flow.react';
import { arraysEqual } from 'src/app/utils/arrays';
import { LinkKind } from 'src/app/schema-diagram/schema-diagram/schema-chart-builder';
import { ResizeObservableService } from '../../services/resize-observable.service';
import { isNullOrUndefined } from '../../utils/utils';

@Component({
  selector: 'app-schema-diagram',
  styleUrls: ['./schema-diagram.component.scss'],
  template: `
    <!-- we need a wrapper to catch the resize events, and then
    provide explicit sizing to container -->
    <h3 *ngIf="title">{{ title }}</h3>
    <div id="wrapper" class="wrapper">
      <div #container></div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SchemaDiagramComponent implements OnInit {

  constructor(
    private router: Router,
    private destroyRef: DestroyRef,
    private resizeObservableService: ResizeObservableService
  ) {
  }

  ngOnInit() {
    const wrapperElement = document.getElementById('wrapper');
    this.resizeObservableService.resizeObservable(wrapperElement)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(resizeEntry => this.onWrapperResized(resizeEntry.contentRect))
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

  @Input()
  title: string;

  @Input()
  memberNameNavigable: boolean = true;

  private _displayedMembers: string[] | 'everything' | 'services' = 'services';
  @Input()
  get displayedMembers(): string[] | 'everything' | 'services' {
    return this._displayedMembers;
  }

  set displayedMembers(value: string[] | 'everything' | 'services') {
    if (this._displayedMembers === value || isNullOrUndefined(value)) {
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

  set schema$(value) {
    if (this._schema$ === value) {
      return;
    }
    this._schema$ = value;
    this.updateComponent();
  }

  @Output()
  fullscreenChange = new EventEmitter();

  private lastMeasureContentRect: DOMRectReadOnly;

  private onWrapperResized(contentRect: DOMRectReadOnly) {
    this.containerRef.nativeElement.width = Math.round(contentRect.width);
    this.containerRef.nativeElement.height = Math.round(contentRect.height);
    this.lastMeasureContentRect = contentRect;
    console.log("onWrapperResized", Math.round(contentRect.width), Math.round(contentRect.height));
    this.updateComponent();
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
    if (this.containerRef && this.containerRef.nativeElement)  {
      console.log('Destroying and rebuilding schema diagram')
      SchemaFlowWrapper.destroy(this.containerRef);
    }
  }

  private updateComponent() {
    if (!this.schema$ || !this.displayedMembers || !this.containerRef || !this.lastMeasureContentRect) {
      return;
    }

    const clickHandler = new Subject<SchemaMemberClickProps>();
    clickHandler
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((clickedMember:SchemaMemberClickProps) => {
        const navigate = (schemaMemberType:SchemaMemberKind, name: string) => {
          switch (schemaMemberType) {
            case 'TYPE':
              this.router.navigate(['catalog',name]);
              break;
            case 'OPERATION':
              const {serviceName, operationName} = splitOperationQualifiedName(name);
              this.router.navigate(['services', serviceName, operationName])
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

    const schemaAndMembersToDisplay$: Observable<SchemaAndRequiredMembersProps> = this.schema$.pipe(
      map(schema => {
        if (this.displayedMembers === 'everything') {
          const membersToDisplay = schema.types
            .filter(t => !t.name.namespace.startsWith('lang.taxi') && !t.name.namespace.startsWith('com.orbitalhq') && !t.isScalar)
            .map(t => t.name.parameterizedName)
            .concat(schema.services.map(s => s.fullyQualifiedName));
          return {
            schema,
            memberNames: membersToDisplay
          }
        } else if (this.displayedMembers === 'services') {
          return {
            schema,
            memberNames: schema.services.map(s => s.fullyQualifiedName)
          }
        } else {
          return { schema, memberNames: this.displayedMembers }
        }
      }));

    SchemaFlowWrapper.initialize(
      this.containerRef,
      schemaAndMembersToDisplay$,
      Math.round(this.lastMeasureContentRect.width),
      Math.round(this.lastMeasureContentRect.height),
      this.visibleLinkKinds,
      clickHandler,
      this.memberNameNavigable,
    )
  }
}

