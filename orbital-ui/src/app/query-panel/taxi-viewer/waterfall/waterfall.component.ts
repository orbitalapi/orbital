import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  input,
  computed,
  effect,
  ViewChild,
  ElementRef,
  AfterViewInit,
  HostListener,
  ChangeDetectorRef
} from "@angular/core";
import { TraceSpanRecord, TraceEventRow } from "src/app/services/query.service";
import { NgForOf, NgIf, DatePipe, JsonPipe } from "@angular/common";

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

interface ConnectionLine {
  x1: number;
  y1: number;
  x2: number;
  y2: number;
  isError: boolean;
}

@Component({
  selector: 'app-waterfall',
  standalone: true,
  imports: [
    NgIf,
    NgForOf,
    DatePipe,
    JsonPipe
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

      <!-- Spans or Detail Panel -->
      <div class="spans-container" #spansContainer *ngIf="!showDetailPanel">
        <!-- SVG overlay for connection lines -->
        <svg
          class="connection-lines-svg"
          [attr.width]="svgWidth"
          [attr.height]="svgHeight"
          *ngIf="connectionLines.length > 0">
          <line
            *ngFor="let line of connectionLines"
            [attr.x1]="line.x1"
            [attr.y1]="line.y1"
            [attr.x2]="line.x2"
            [attr.y2]="line.y2"
            class="connection-line"
            [class.error-connection]="line.isError"
            stroke-width="2"
            marker-end="url(#arrowhead)">
          </line>

          <!-- Arrow marker definition -->
          <defs>
            <marker
              id="arrowhead"
              markerWidth="6"
              markerHeight="4"
              refX="5"
              refY="2"
              orient="auto">
              <polygon
                points="0 0, 6 2, 0 4"
                class="arrow-marker" />
            </marker>
          </defs>
        </svg>

        <div
          *ngFor="let span of waterfallSpans; let spanIndex = index; trackBy: trackBySpanId"
          class="span-row"
          [class.has-errors]="span.hasErrors"
          [class.incomplete]="!span.isComplete"
          [attr.data-span-index]="spanIndex"
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
              [title]="getSpanTooltip(span)"
              (click)="onSpanBarClick($event, span)">

              <!-- Event markers for all events in the span -->
              <div
                *ngFor="let marker of span.eventMarkers; let eventIndex = index; trackBy: trackByEventId"
                class="event-marker"
                [class.linked-event]="marker.isLinked"
                [class.error-event]="marker.event.tracingEventKind === 'ERROR'"
                [style.left.%]="marker.positionPercent"
                [attr.data-event-id]="marker.event.eventId"
                [attr.data-span-index]="spanIndex"
                [attr.data-event-index]="eventIndex"
                [title]="getEventTooltip(marker.event)"
                (click)="onEventClick($event, marker.event)">
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Detail Panel -->
      <div class="detail-panel" *ngIf="showDetailPanel">
        <div class="detail-header">
          <h3 class="detail-title">{{ getDetailTitle() }}</h3>
          <button class="close-button" (click)="closeDetailPanel()" title="Close">
            <span>&times;</span>
          </button>
        </div>

        <div class="detail-content">
          <!-- Span Details -->
          <div *ngIf="isSpanSelected()" class="span-details">
            <div class="detail-section">
              <h4>Span Information</h4>
              <div class="detail-row">
                <label>Span ID:</label>
                <span class="mono">{{ getSelectedSpan()!.spanId }}</span>
              </div>
              <div class="detail-row">
                <label>Parent Span:</label>
                <span class="mono">{{ getSelectedSpan()!.parentSpanId || 'None' }}</span>
              </div>
              <div class="detail-row">
                <label>Resource:</label>
                <span>{{ getSelectedSpan()!.eventResource }}</span>
              </div>
              <div class="detail-row">
                <label>Verb:</label>
                <span>{{ getSelectedSpan()!.eventVerb }}</span>
              </div>
              <div class="detail-row">
                <label>Source:</label>
                <span>{{ getSelectedSpan()!.eventSourceQualifiedName }}</span>
              </div>
            </div>

            <div class="detail-section">
              <h4>Timing</h4>
              <div class="detail-row">
                <label>Start Time:</label>
                <span>{{ getSelectedSpan()!.firstEventTimestamp | date:'medium' }}</span>
              </div>
              <div class="detail-row">
                <label>End Time:</label>
                <span>{{ getSelectedSpan()!.lastEventTimestamp | date:'medium' }}</span>
              </div>
              <div class="detail-row">
                <label>Duration:</label>
                <span>{{ formatDuration(getSelectedSpan()!.durationMs) }}</span>
              </div>
              <div class="detail-row">
                <label>Offset:</label>
                <span>{{ formatDuration(getSelectedSpan()!.offsetMs) }}</span>
              </div>
            </div>

            <div class="detail-section">
              <h4>Status</h4>
              <div class="detail-row">
                <label>Complete:</label>
                <span class="status-badge" [class.complete]="getSelectedSpan()!.isComplete" [class.incomplete]="!getSelectedSpan()!.isComplete">
                  {{ getSelectedSpan()!.isComplete ? 'Yes' : 'No' }}
                </span>
              </div>
              <div class="detail-row">
                <label>Has Errors:</label>
                <span class="status-badge" [class.error]="getSelectedSpan()!.hasErrors" [class.success]="!getSelectedSpan()!.hasErrors">
                  {{ getSelectedSpan()!.hasErrors ? 'Yes' : 'No' }}
                </span>
              </div>
            </div>

            <div class="detail-section">
              <h4>Events ({{ getSelectedSpan()!.events.length }})</h4>
              <div class="events-list">
                <div
                  *ngFor="let event of getSelectedSpan()!.events; trackBy: trackByEventRowId"
                  class="event-item"
                  [class.error-event]="event.tracingEventKind === 'ERROR'"
                  (click)="onEventClick($event, event)">
                  <div class="event-summary">
                    <span class="event-verb">{{ event.eventVerb }}</span>
                    <span class="event-resource">{{ event.eventResource }}</span>
                    <span class="event-time">{{ event.timestamp | date:'HH:mm:ss.SSS' }}</span>
                  </div>
                  <div class="event-meta">
                    <span class="event-kind">{{ event.tracingEventKind }}</span>
                    <span class="event-state">{{ event.spanState }}</span>
                    <span *ngIf="event.linkedEventId" class="linked-indicator">🔗 Linked</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Event Details -->
          <div *ngIf="isEventSelected()" class="event-details">
            <div class="detail-section">
              <h4>Event Information</h4>
              <div class="detail-row">
                <label>Event ID:</label>
                <span class="mono">{{ getSelectedEvent()!.eventId }}</span>
              </div>
              <div class="detail-row">
                <label>Span ID:</label>
                <span class="mono">{{ getSelectedEvent()!.spanId }}</span>
              </div>
              <div class="detail-row">
                <label>Resource:</label>
                <span>{{ getSelectedEvent()!.eventResource }}</span>
              </div>
              <div class="detail-row">
                <label>Verb:</label>
                <span>{{ getSelectedEvent()!.eventVerb }}</span>
              </div>
              <div class="detail-row">
                <label>Source:</label>
                <span>{{ getSelectedEvent()!.eventSourceQualifiedName }}</span>
              </div>
            </div>

            <div class="detail-section">
              <h4>Status & Timing</h4>
              <div class="detail-row">
                <label>Kind:</label>
                <span class="status-badge" [class.error]="getSelectedEvent()!.tracingEventKind === 'ERROR'" [class.success]="getSelectedEvent()!.tracingEventKind === 'OK'">
                  {{ getSelectedEvent()!.tracingEventKind }}
                </span>
              </div>
              <div class="detail-row">
                <label>State:</label>
                <span>{{ getSelectedEvent()!.spanState }}</span>
              </div>
              <div class="detail-row">
                <label>Timestamp:</label>
                <span>{{ getSelectedEvent()!.timestamp | date:'medium' }}</span>
              </div>
              <div class="detail-row" *ngIf="getSelectedEvent()!.linkedEventId">
                <label>Linked Event:</label>
                <span class="mono">{{ getSelectedEvent()!.linkedEventId }}</span>
              </div>
            </div>

            <div class="detail-section" *ngIf="getSelectedEvent()!.exchangeMetadata">
              <h4>Metadata</h4>
              <pre class="metadata-content">{{ getSelectedEvent()!.exchangeMetadata | json }}</pre>
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
export class WaterfallComponent implements AfterViewInit {

  spanInitialPadding = 8; // pixels.
  spanStepPadding = 20; // pixels. Additional padding for each level of nesting

  traceSpans = input<TraceSpanRecord[]>([])
  showSummary = input<boolean>(true);
  maxLabelWidth = input<number>(300);// pixels

  @Output() spanClick = new EventEmitter<TraceSpanRecord>();
  @Output() eventClick = new EventEmitter<TraceEventRow>();
  @ViewChild('spansContainer', { static: false }) spansContainer!: ElementRef<HTMLDivElement>;

  waterfallSpans: WaterfallSpan[] = [];
  timeMarkers: { position: number; label: string }[] = [];
  totalDurationMs: number = 0;
  errorCount: number = 0;
  connectionLines: ConnectionLine[] = [];
  svgWidth: number = 0;
  svgHeight: number = 0;

  // Detail panel state
  selectedItem: TraceSpanRecord | TraceEventRow | null = null;
  showDetailPanel: boolean = false;


  constructor(private cdr: ChangeDetectorRef) {
    effect(() => {
      const spans = this.traceSpans();
      this.calculateWaterfallData(spans);
      // Use longer delay to ensure DOM is fully rendered
      setTimeout(() => {
        this.calculateConnectionLines();
        this.cdr.detectChanges(); // Force change detection
      }, 50);
    })
  }

  ngAfterViewInit() {
    // Calculate once after view is initialized
    setTimeout(() => {
      this.calculateConnectionLines();
      this.cdr.detectChanges();
    }, 100);
  }

  @HostListener('window:resize')
  onWindowResize() {
    setTimeout(() => {
      this.calculateConnectionLines();
      this.cdr.detectChanges();
    }, 10);
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

  private calculateConnectionLines(): void {
    if (!this.spansContainer || this.waterfallSpans.length === 0) {
      this.connectionLines = [];
      this.svgWidth = 0;
      this.svgHeight = 0;
      return;
    }

    const container = this.spansContainer.nativeElement;

    // Ensure container has rendered content
    if (container.scrollWidth === 0 || container.scrollHeight === 0) {
      // Retry after a short delay if container isn't ready
      setTimeout(() => this.calculateConnectionLines(), 10);
      return;
    }

    const containerRect = container.getBoundingClientRect();

    // Update SVG dimensions
    this.svgWidth = container.scrollWidth;
    this.svgHeight = container.scrollHeight;

    const lines: ConnectionLine[] = [];
    const eventIdToPosition = new Map<string, { x: number; y: number; isError: boolean }>();

    // First pass: collect all event positions
    this.waterfallSpans.forEach((span, spanIndex) => {
      span.eventMarkers.forEach((marker, eventIndex) => {
        const eventElement = container.querySelector(
          `[data-span-index="${spanIndex}"] [data-event-index="${eventIndex}"]`
        ) as HTMLElement;

        if (eventElement) {
          const eventRect = eventElement.getBoundingClientRect();
          const relativeX = eventRect.left - containerRect.left + eventElement.offsetWidth / 2;
          const relativeY = eventRect.top - containerRect.top + eventElement.offsetHeight / 2;

          eventIdToPosition.set(marker.event.eventId, {
            x: relativeX,
            y: relativeY,
            isError: marker.event.tracingEventKind === 'ERROR'
          });
        }
      });
    });

    // Second pass: create lines for linked events
    this.waterfallSpans.forEach(span => {
      span.eventMarkers.forEach(marker => {
        if (marker.event.linkedEventId) {
          const sourcePos = eventIdToPosition.get(marker.event.linkedEventId);
          const targetPos = eventIdToPosition.get(marker.event.eventId);

          if (sourcePos && targetPos) {
            lines.push({
              x1: sourcePos.x,
              y1: sourcePos.y,
              x2: targetPos.x,
              y2: targetPos.y,
              isError: sourcePos.isError || targetPos.isError
            });
          }
        }
      });
    });

    this.connectionLines = lines;
  }

  onSpanClick(span: TraceSpanRecord): void {
    this.selectedItem = span;
    this.showDetailPanel = true;
    this.spanClick.emit(span);
  }

  onSpanBarClick(event: Event, span: TraceSpanRecord): void {
    event.stopPropagation();
    this.onSpanClick(span);
  }

  onEventClick(event: Event, eventItem: TraceEventRow): void {
    event.stopPropagation();
    this.selectedItem = eventItem;
    this.showDetailPanel = true;
    this.eventClick.emit(eventItem);
  }

  closeDetailPanel(): void {
    this.showDetailPanel = false;
    this.selectedItem = null;
  }

  isSpanSelected(): boolean {
    return this.selectedItem !== null && 'spanId' in this.selectedItem && 'events' in this.selectedItem;
  }

  isEventSelected(): boolean {
    return this.selectedItem !== null && 'eventId' in this.selectedItem && !('events' in this.selectedItem);
  }

  getSelectedSpan(): TraceSpanRecord | null {
    return this.isSpanSelected() ? this.selectedItem as TraceSpanRecord : null;
  }

  getSelectedEvent(): TraceEventRow | null {
    return this.isEventSelected() ? this.selectedItem as TraceEventRow : null;
  }

  getDetailTitle(): string {
    if (this.isSpanSelected()) {
      const span = this.getSelectedSpan()!;
      return `${span.eventVerb} ${span.eventResource}`;
    } else if (this.isEventSelected()) {
      const event = this.getSelectedEvent()!;
      return `${event.eventVerb} ${event.eventResource}`;
    }
    return 'Details';
  }

  trackBySpanId(index: number, span: WaterfallSpan): string {
    return span.spanId;
  }

  trackByEventId(index: number, marker: EventMarker): string {
    return marker.event.eventId;
  }
  trackByEventRowId(index: number, event: TraceEventRow): string {
    return event.eventId;
  }
}
