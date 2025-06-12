import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  input,
  computed,
  effect
} from "@angular/core";
import { TraceEventRow, TraceSpanRecord } from "src/app/services/query.service";
import { NgForOf, NgIf } from "@angular/common";

interface WaterfallSpan extends TraceSpanRecord {
  depth: number;
  leftPercent: number;
  widthPercent: number;
  eventMarkers: EventMarker[];
}

interface EventMarker {
  event: TraceEventRow;
  positionPercent: number;
  isLinked: boolean;
}

@Component({
  selector: 'app-waterfall',
  standalone: true,
  imports: [
    NgIf,
    NgForOf
  ],
  template: `
    <div class="waterfall-container" *ngIf="waterfallSpans.length > 0">
      <!-- Time scale header -->
      <div class="time-scale">
        <!-- align for the labels column -->
        <div class="spacer"></div>
        <div class="time-markers">
          <div
            *ngFor="let marker of timeMarkers"
            class="time-marker"
            [style.left.%]="marker.position">
            <span class="time-label">{{ marker.label }}</span>
          </div>
        </div>
      </div>

      <!-- Spans -->
      <div class="spans-container">
        <div
          *ngFor="let span of waterfallSpans; trackBy: trackBySpanId"
          class="span-row"
          [class.has-errors]="span.hasErrors"
          [class.incomplete]="!span.isComplete"
          (click)="onSpanClick(span)">

          <!-- Span label with indentation -->
          <div class="span-label" [style.padding-left.px]="(span.depth * spanStepPadding) + spanInitialPadding">
            <div class="span-name" [title]="span.eventSourceQualifiedName">
              {{ getSpanDisplayName(span) }}
            </div>
            <div class="span-duration">{{ formatDuration(span.durationMs) }}</div>
          </div>

          <!-- Span timeline bar -->
          <div class="span-timeline">
            <div
              class="span-bar"
              [class.error-bar]="span.hasErrors"
              [class.incomplete-bar]="!span.isComplete"
              [style.left.%]="span.leftPercent"
              [style.width.%]="span.widthPercent"
              [title]="getSpanTooltip(span)">

              <!-- Event markers for all events in the span -->
              <div
                *ngFor="let marker of span.eventMarkers; trackBy: trackByEventId"
                class="event-marker"
                [class.linked-event]="marker.isLinked"
                [class.error-event]="marker.event.tracingEventKind === 'ERROR'"
                [style.left.%]="marker.positionPercent"
                [title]="getEventTooltip(marker.event)">
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Summary -->
      <div class="waterfall-summary" *ngIf="showSummary">
        <span>Total: {{ waterfallSpans.length }} spans</span>
        <span>Duration: {{ formatDuration(totalDurationMs) }}</span>
        <span *ngIf="errorCount > 0" class="error-count">{{ errorCount }} errors</span>
      </div>
    </div>

    <div class="empty-state" *ngIf="waterfallSpans.length === 0">
      <p>No trace data available</p>
    </div>
  `,
  styleUrl: './waterfall.component.scss'
})
export class WaterfallComponent {

  spanInitialPadding = 8; // pixels.
  spanStepPadding = 20; // pixels. Additional padding for each level of nesting

  traceSpans = input<TraceSpanRecord[]>([])
  showSummary = input<boolean>(true);
  maxLabelWidth = input<number>(300);// pixels

  @Output() spanClick = new EventEmitter<TraceSpanRecord>();

  waterfallSpans: WaterfallSpan[] = [];
  timeMarkers: { position: number; label: string }[] = [];
  totalDurationMs: number = 0;
  errorCount: number = 0;


  constructor() {
    effect(() => {
      const spans = this.traceSpans();
      this.calculateWaterfallData(spans)
    })
  }


  private calculateWaterfallData(traceSpans: TraceSpanRecord[]): void {
    if (!traceSpans || traceSpans.length === 0) {
      this.waterfallSpans = [];
      this.timeMarkers = [];
      this.totalDurationMs = 0;
      this.errorCount = 0;
      return;
    }

    // Flatten spans while maintaining hierarchy information
    const flatSpans = this.flattenSpansWithDepth(traceSpans);

    // Calculate total duration
    const allSpans = flatSpans.map(fs => fs.span);
    const earliestStart = Math.min(...allSpans.map(s => s.offsetMs));
    const latestEnd = Math.max(...allSpans.map(s => s.offsetMs + s.durationMs));
    this.totalDurationMs = latestEnd - earliestStart;

    // Calculate positions for each span
    this.waterfallSpans = flatSpans.map(({ span, depth }) => {
      const leftPercent = this.totalDurationMs > 0 ? (span.offsetMs / this.totalDurationMs) * 100 : 0;
      const widthPercent = this.totalDurationMs > 0 ? (span.durationMs / this.totalDurationMs) * 100 : 0;

      // Calculate event markers for this span
      const eventMarkers = this.calculateEventMarkers(span);

      return {
        ...span,
        depth,
        leftPercent: Math.max(0, leftPercent),
        widthPercent: Math.max(0.1, widthPercent), // Minimum width for visibility
        eventMarkers
      };
    });

    // Calculate error count
    this.errorCount = this.waterfallSpans.filter(s => s.hasErrors).length;

    // Generate time markers
    this.generateTimeMarkers();
  }

  private calculateEventMarkers(span: TraceSpanRecord): EventMarker[] {
    if (!span.events || span.events.length === 0) {
      return [];
    }

    const spanStartMs = span.offsetMs;
    const spanDurationMs = span.durationMs;

    return span.events.map(event => {
      // Calculate event position relative to the trace start
      const eventOffsetMs = (event.timestamp.getTime() - span.traceStartTime.getTime());
      // Calculate position within the span bar (0-100%)
      const positionWithinSpan = spanDurationMs > 0
        ? ((eventOffsetMs - spanStartMs) / spanDurationMs) * 100
        : 0;

      return {
        event,
        positionPercent: Math.max(0, Math.min(100, positionWithinSpan)),
        isLinked: !!event.linkedEventId
      };
    });
  }

  private flattenSpansWithDepth(spans: TraceSpanRecord[], depth: number = 0): { span: TraceSpanRecord; depth: number }[] {
    const result: { span: TraceSpanRecord; depth: number }[] = [];

    // Sort spans by start time
    const sortedSpans = [...spans].sort((a, b) => a.offsetMs - b.offsetMs);

    for (const span of sortedSpans) {
      result.push({ span, depth });

      // Add children recursively
      if (span.children && span.children.length > 0) {
        result.push(...this.flattenSpansWithDepth(span.children, depth + 1));
      }
    }

    return result;
  }

  private generateTimeMarkers(): void {
    if (this.totalDurationMs <= 0) {
      this.timeMarkers = [];
      return;
    }

    const markerCount = 6; // Number of time markers
    this.timeMarkers = [];

    for (let i = 0; i <= markerCount; i++) {
      const position = (i / markerCount) * 100;
      const timeMs = (i / markerCount) * this.totalDurationMs;
      const label = this.formatDuration(timeMs);

      this.timeMarkers.push({ position, label });
    }
  }

  formatDuration(ms: number): string {
    if (ms < 1000) {
      return `${Math.round(ms)}ms`;
    } else if (ms < 60000) {
      return `${(ms / 1000).toFixed(2)}s`;
    } else {
      const minutes = Math.floor(ms / 60000);
      const seconds = ((ms % 60000) / 1000).toFixed(1);
      return `${minutes}m ${seconds}s`;
    }
  }

  getSpanDisplayName(span: TraceSpanRecord): string {
    return `${span.eventVerb} ${span.eventResource}`;
  }

  getSpanTooltip(span: TraceSpanRecord): string {
    const lines = [
      `Span: ${span.spanId}`,
      `Resource: ${span.eventResource}`,
      `Duration: ${this.formatDuration(span.durationMs)}`,
      `Offset: ${this.formatDuration(span.offsetMs)}`,
      `Events: ${span.events.length}`,
    ];

    if (span.hasErrors) {
      lines.push('⚠️ Contains errors');
    }

    if (!span.isComplete) {
      lines.push('⏳ Incomplete');
    }

    return lines.join('\n');
  }

  getEventTooltip(event: TraceEventRow): string {
    const lines = [
      `Event: ${event.eventId}`,
      `${event.eventVerb} ${event.eventResource}`,
      `Kind: ${event.tracingEventKind}`,
      `State: ${event.spanState}`,
      `Time: ${event.timestamp.toISOString()}`
    ];

    if (event.linkedEventId) {
      lines.push(`Linked to: ${event.linkedEventId}`);
    }

    return lines.join('\n');
  }

  onSpanClick(span: TraceSpanRecord): void {
    this.spanClick.emit(span);
  }

  trackBySpanId(index: number, span: WaterfallSpan): string {
    return span.spanId;
  }

  trackByEventId(index: number, marker: EventMarker): string {
    return marker.event.eventId;
  }
}
