import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  Injector,
  input,
  ViewChild,
} from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { QueryPlan } from 'src/app/services/query.service';
import { CompilationMessage } from 'src/app/services/schema';
import { QueryPlanFlowWrapper } from './query-plan-flow.react';
import { ResizeObservableService } from '../services/resize-observable.service';

@Component({
  selector: 'app-query-plan-diagram',
  standalone: true,
  imports: [],
  template: `
    <div #wrapperElementRef class="wrapper">
      <div #container></div>
    </div>
  `,
  styleUrl: './query-plan-diagram.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [ResizeObservableService],
})
export class QueryPlanDiagramComponent implements AfterViewInit {
  @ViewChild('wrapperElementRef') wrapperElement!: ElementRef;
  @ViewChild('container') containerRef!: ElementRef;

  queryPlan = input<QueryPlan>();
  compilationMessages = input<CompilationMessage[]>();

  private lastMeasureContentRect: DOMRectReadOnly;
  private isInitialized = false;

  constructor(
    private destroyRef: DestroyRef,
    private resizeObservableService: ResizeObservableService,
    private injector: Injector
  ) {}

  ngAfterViewInit() {
    const wrapperElement = this.wrapperElement.nativeElement;

    // Get initial dimensions immediately
    const rect = wrapperElement.getBoundingClientRect();
    this.lastMeasureContentRect = new DOMRectReadOnly(rect.x, rect.y, rect.width, rect.height);

    this.resizeObservableService
      .resizeObservable(wrapperElement)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((resizeEntry) => this.onWrapperResized(resizeEntry.contentRect));

    // Subscribe to changes in queryPlan input
    toObservable(this.queryPlan, { injector: this.injector })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        console.log('queryPlan input changed', this.queryPlan());
        this.updateComponent();
      });

    // Subscribe to changes in compilationMessages input
    toObservable(this.compilationMessages, { injector: this.injector })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        console.log('compilationMessages input changed', this.compilationMessages());
        this.updateComponent();
      });

    this.updateComponent();
  }

  private onWrapperResized(contentRect: DOMRectReadOnly) {
    this.containerRef.nativeElement.width = Math.round(contentRect.width);
    this.containerRef.nativeElement.height = Math.round(contentRect.height);
    this.lastMeasureContentRect = contentRect;
    console.log('onWrapperResized', Math.round(contentRect.width), Math.round(contentRect.height));
    this.updateComponent();
  }

  private updateComponent() {
    if (!this.containerRef || !this.lastMeasureContentRect) {
      return;
    }

    const queryPlan = this.queryPlan();
    if (!queryPlan) {
      return;
    }

    const queryPlan$ = toObservable(this.queryPlan, { injector: this.injector });
    const compilationMessages$ = toObservable(this.compilationMessages, { injector: this.injector });

    QueryPlanFlowWrapper.initialize(
      this.containerRef,
      queryPlan$,
      compilationMessages$,
      Math.round(this.lastMeasureContentRect.width),
      Math.round(this.lastMeasureContentRect.height)
    );

    this.isInitialized = true;
  }
}
