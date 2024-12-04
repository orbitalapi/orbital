import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component, computed,
  DestroyRef,
  ElementRef,
  EventEmitter, Injector, input,
  Input, model,
  Output, signal,
  ViewChild, WritableSignal,
} from '@angular/core';
import { Router } from '@angular/router';
import {takeUntilDestroyed, toObservable} from '@angular/core/rxjs-interop';
import {combineLatest, Observable, Subject} from 'rxjs';
import {map} from 'rxjs/operators';
import {SchemaDiagramSpec} from '../schema-diagram-markdown-wrapper/schema-diagram-markdown-wrapper.component';
import {QualifiedName, Schema, SchemaMemberKind, splitOperationQualifiedName} from '../services/schema';
import {
  ClickHandlerPayload,
  SchemaAndRequiredMembersProps,
  SchemaFlowWrapper,
} from './schema-flow.react';
import { arraysEqual } from 'src/app/utils/arrays';
import { LinkKind } from 'src/app/schema-diagram/schema-chart-builder';
import { ResizeObservableService } from '../services/resize-observable.service';

@Component({
  selector: 'app-schema-diagram',
  styleUrls: ['./schema-diagram.component.scss'],
  template: `
    <!-- we need a wrapper to catch the resize events, and then
    provide explicit sizing to container -->
    <h3 *ngIf="title">{{ title }}</h3>
    <div #wrapperElementRef class="wrapper" [class.has-border]="hasBorder">
      <div class="toolbar" [class.is-full-screen]="isFullScreen$ | async">
        <app-schema-multi-select
          *ngIf="showTypeToolbar"
          [schema]="schema$ | async"
          [schemaQualifiedNames]="schemaQualifiedNames()"
          [selectedMembers]="computedDisplayedMembers()"
          (selectedMembersChange)="displayedMembers.set($event)"
        ></app-schema-multi-select>
      </div>
      <div #container></div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [ResizeObservableService]
})
export class SchemaDiagramComponent implements AfterViewInit {
  @ViewChild('wrapperElementRef') wrapperElement!: ElementRef;

  constructor(
    private router: Router,
    private destroyRef: DestroyRef,
    private resizeObservableService: ResizeObservableService,
    private injector: Injector
  ) {
  }

  ngAfterViewInit() {
    const wrapperElement = this.wrapperElement.nativeElement;
    this.resizeObservableService.resizeObservable(wrapperElement)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(resizeEntry => this.onWrapperResized(resizeEntry.contentRect))

    this.schema$.pipe(
      takeUntilDestroyed(this.destroyRef)
    )
      .subscribe(schema => {
        this.schemaQualifiedNames.set(schema.types
          .filter(t => !t.name.namespace.startsWith('lang.taxi') && !t.name.namespace.startsWith('com.orbitalhq'))
          .map(t => t.name)
          .concat(schema.services.map(s => s.name))
          .sort((a, b) => a.shortDisplayName.localeCompare(b.shortDisplayName))
        );
      })
  }

  @Input()
  showTypeToolbar: boolean = true

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

  @Input()
  hasBorder: boolean;

  displayedMembers = model<string[] | 'everything' | 'services'>('services')
  displayedMemberPositions = input<Omit<SchemaDiagramSpec, 'showTypeToolbar'>>()
  computedDisplayedMembers = computed(() => Array.isArray(this.displayedMembers()) ? this.displayedMembers() as string[] : [])
  schemaQualifiedNames: WritableSignal<QualifiedName[]> = signal([])

  isFullScreen$ = new Subject<boolean>()

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
    if (!this.schema$ || !this.displayedMembers() || !this.containerRef || !this.lastMeasureContentRect) {
      return;
    }

    const clickHandler = new Subject<ClickHandlerPayload>();
    clickHandler
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((event: ClickHandlerPayload) => {
        switch (event.command) {
          case 'navigate':
            const navigate = (schemaMemberType: SchemaMemberKind, name: string) => {
              switch (schemaMemberType) {
                case 'TYPE':
                  this.router.navigate(['catalog', name]);
                  break;
                case 'OPERATION':
                  const { serviceName, operationName } = splitOperationQualifiedName(name);
                  this.router.navigate(['services', serviceName, operationName])
                  break;
                case 'SERVICE':
                  this.router.navigate(['services', name])
                  break;
              }
            }
            if ("type" in event.member) {
              navigate(event.member.type, event.member.name.memberQualifiedName.fullyQualifiedName);
            } else {
              navigate(event.member.kind, event.member.name.fullyQualifiedName);
            }
            return;
          case 'delete':
            const fullyQualifiedName = "type" in event.member ?
              event.member.name.memberQualifiedName.fullyQualifiedName :
              event.member.name.fullyQualifiedName;
            console.log('delete the following member(s)', fullyQualifiedName)
            if (Array.isArray(this.displayedMembers()) && fullyQualifiedName) {
              this.displayedMembers.update(members => {
                return (members as string[]).filter(member => member !== fullyQualifiedName)
              })
            }
        }
      })

    const memberUpdatedHandler = new Subject<Set<string>>()
    memberUpdatedHandler
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(event => {
        console.log(event)
        this.displayedMembers.set(Array.from(event))
      })

    const schemaAndMembersToDisplay$: Observable<SchemaAndRequiredMembersProps> =
      combineLatest([this.schema$, toObservable(this.displayedMembers, {injector: this.injector})])
        .pipe(
          takeUntilDestroyed(this.destroyRef),
          map(([schema, displayedMembers]) => {
            if (displayedMembers === 'everything') {
              const memberNames = schema.types
                .filter(t => !t.name.namespace.startsWith('lang.taxi') && !t.name.namespace.startsWith('com.orbitalhq') && !t.isScalar)
                .map(t => t.name.parameterizedName)
                .concat(schema.services.map(s => s.fullyQualifiedName));
              this.displayedMembers.set(memberNames)
              return {
                schema,
                memberNames
              }
            } else if (displayedMembers === 'services') {
              const memberNames = schema.services.map(s => s.fullyQualifiedName)
              this.displayedMembers.set(memberNames)
              return {
                schema,
                memberNames
              }
            } else {
              return { schema, memberNames: displayedMembers, memberNamePositions: this.displayedMemberPositions() }
            }
          })
        );

    SchemaFlowWrapper.initialize(
      this.containerRef,
      schemaAndMembersToDisplay$,
      Math.round(this.lastMeasureContentRect.width),
      Math.round(this.lastMeasureContentRect.height),
      this.visibleLinkKinds,
      clickHandler,
      memberUpdatedHandler,
      this.isFullScreen$,
      this.memberNameNavigable,
    )
  }
}

