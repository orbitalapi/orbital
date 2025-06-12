import { Component, EventEmitter, Input, Output } from "@angular/core";
import { TraceSpanRecord, TraceEventRow } from "src/app/services/query.service";
import { NgForOf, NgIf, DatePipe, JsonPipe } from "@angular/common";
import { formatDuration } from "src/app/query-panel/taxi-viewer/waterfall/waterfall.component";

@Component({
  selector: 'app-trace-detail',
  standalone: true,
  imports: [NgIf, NgForOf, DatePipe, JsonPipe],
  template: `
    <div class="detail-panel">
      <div class="detail-header">
        <h3 class="detail-title">{{ getDetailTitle() }}</h3>
        <button class="close-button" (click)="onClose()" title="Close">
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
                (click)="onEventClick(event)">
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
  `,
  styleUrl: './trace-detail.component.scss'
})
export class TraceDetailComponent {
  @Input() selectedItem: TraceSpanRecord | TraceEventRow | null = null;
  @Output() close = new EventEmitter<void>();
  @Output() eventClick = new EventEmitter<TraceEventRow>();

  onClose(): void {
    this.close.emit();
  }

  onEventClick(event: TraceEventRow): void {
    this.eventClick.emit(event);
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

  formatDuration(ms: number): string {
    return formatDuration(ms)
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



  trackByEventRowId(index: number, event: TraceEventRow): string {
    return event.eventId;
  }
}
