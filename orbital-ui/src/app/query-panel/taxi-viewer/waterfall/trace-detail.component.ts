import { Component, EventEmitter, Input, Output } from "@angular/core";
import { TraceSpanRecord, TraceEventRow } from "src/app/services/query.service";
import { NgForOf, NgIf, DatePipe, JsonPipe, NgSwitch, NgSwitchCase } from "@angular/common";

@Component({
  selector: 'app-trace-detail',
  standalone: true,
  imports: [NgIf, NgForOf, DatePipe, JsonPipe, NgSwitch, NgSwitchCase],
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

            <!-- Other metadata properties (excluding type and payload) -->
            <div class="metadata-table" *ngIf="getFilteredMetadata().length > 0">
              <div class="metadata-row" *ngFor="let item of getFilteredMetadata()">
                <div class="metadata-key">{{ item.key }}</div>
                <div class="metadata-value">{{ item.value }}</div>
              </div>
            </div>

            <!-- Payload section -->
            <div class="payload-section" *ngIf="hasPayload()">
              <h5>Payload</h5>
              <div class="payload-content" [ngSwitch]="getPayloadType()">
                <!-- JSON payload -->
                <pre *ngSwitchCase="'json'" class="payload-json">{{ getFormattedPayload() }}</pre>
                <!-- String payload -->
                <div *ngSwitchCase="'string'" class="payload-string">{{ getPayloadString() }}</div>
                <!-- Null payload -->
                <div *ngSwitchCase="'null'" class="payload-null">No payload</div>
              </div>
            </div>
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

  trackByEventRowId(index: number, event: TraceEventRow): string {
    return event.eventId;
  }

  getFilteredMetadata(): Array<{key: string, value: any}> {
    const event = this.getSelectedEvent();
    if (!event?.exchangeMetadata) return [];

    return Object.entries(event.exchangeMetadata)
      .filter(([key]) => key !== 'type' && key !== 'payload')
      .map(([key, value]) => ({ key, value: this.formatMetadataValue(value) }));
  }

  private formatMetadataValue(value: any): string {
    if (value === null || value === undefined) return 'null';
    if (typeof value === 'object') return JSON.stringify(value);
    return String(value);
  }

  hasPayload(): boolean {
    const event = this.getSelectedEvent();
    return event?.exchangeMetadata && 'payload' in event.exchangeMetadata;
  }

  getPayloadType(): 'json' | 'string' | 'null' {
    const event = this.getSelectedEvent();
    if (!event?.exchangeMetadata) return 'null';

    const payload = event.exchangeMetadata.payload;
    if (payload === null || payload === undefined) return 'null';

    if (typeof payload === 'string') {
      // Try to detect if it's a JSON-escaped string
      if (payload.trim().startsWith('{') || payload.trim().startsWith('[')) {
        try {
          JSON.parse(payload);
          return 'json';
        } catch {
          // Could be truncated JSON, still treat as JSON for display
          return 'json';
        }
      }
      return 'string';
    }

    // If it's already an object, treat as JSON
    if (typeof payload === 'object') return 'json';

    return 'string';
  }

  getFormattedPayload(): string {
    const event = this.getSelectedEvent();
    if (!event?.exchangeMetadata) return '';

    const payload = event.exchangeMetadata.payload;
    if (typeof payload === 'string') {
      try {
        // Try to parse and re-stringify for proper formatting
        const parsed = JSON.parse(payload);
        return JSON.stringify(parsed, null, 2);
      } catch {
        // If parsing fails (e.g., truncated), return the raw string with basic formatting
        return payload;
      }
    }

    if (typeof payload === 'object') {
      return JSON.stringify(payload, null, 2);
    }

    return String(payload);
  }

  getPayloadString(): string {
    const event = this.getSelectedEvent();
    if (!event?.exchangeMetadata) return '';

    const payload = event.exchangeMetadata.payload;
    return String(payload);
  }
}
