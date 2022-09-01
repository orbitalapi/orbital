import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, Input, OnInit, ViewChild } from '@angular/core';
import { Schema } from '../../services/schema';
import { SchemaFlowWrapper } from './schema-flow.react';
import { ResizedEvent } from 'angular-resize-event/lib/resized.event';
import { isNullOrUndefined } from 'util';

@Component({
  selector: 'app-schema-diagram',
  styleUrls: ['./schema-diagram.component.scss'],
  template: `
    <!-- we need a wrapper to catch the resize events, and then
    provide explicit sizing to container -->
    <div class="wrapper" (resized)="onWrapperResized($event)">
      <div #container></div>
    </div>

  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SchemaDiagramComponent implements AfterViewInit {

  ngAfterViewInit(): void {
  }

  private _displayedMembers: string[];

  @Input()
  get displayedMembers(): string[] {
    return this._displayedMembers;
  }

  set displayedMembers(value: string[]) {
    this._displayedMembers = value;
    this.resetComponent();
  }

  @Input()
  schema: Schema;

  private lastMeasureEvent: ResizedEvent | null = null;

  onWrapperResized(event: ResizedEvent) {
    console.log('resize', event)

    if (!this.isSignificantResize(event)) {
      return;
    }

    this.lastMeasureEvent = event;

    this.containerRef.nativeElement.width = event.newRect.width;
    this.containerRef.nativeElement.height = event.newRect.height;

    console.log(`resizing to width: ${event.newRect.width}, height: ${event.newRect.height}`)
    this.resetComponent();
  }

  private isSignificantResize(event: ResizedEvent) {
    if (!this.lastMeasureEvent) {
      return true;
    }
    if (isNullOrUndefined(event.newRect.height) || isNullOrUndefined(event.newRect.width)) {
      return false;
    }
    return (Math.abs(event.newRect.height - this.lastMeasureEvent.newRect.height) > 100 ||
      Math.abs(event.newRect.width - this.lastMeasureEvent.newRect.width) > 100);
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
    this.resetComponent();
  }

  resetComponent() {
    if (!this.schema || !this.displayedMembers || !this.containerRef || !this.lastMeasureEvent) {
      return;
    }
    // const controller = new SchemaChartController(this.schema);
    // this.displayedMembers.forEach(member => {
    //   controller.ensureMemberPresent(findSchemaMember(this.schema, member))
    // })

    SchemaFlowWrapper.initialize(
      this._containerRef,
      this.displayedMembers,
      this.schema,
      this.lastMeasureEvent.newRect.width,
      this.lastMeasureEvent.newRect.height
    )
  }

}

