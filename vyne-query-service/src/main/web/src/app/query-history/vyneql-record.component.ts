import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {QueryHistorySummary} from '../services/query.service';

@Component({
  selector: 'app-vyneql-record',
  template: `
    <div class="history-item">
      <code>{{ displayedQuery | truncate: 300 }}</code>
    </div>
  `,
  styleUrls: ['./vyneql-record.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class VyneqlRecordComponent {

  @Input()
  taxiQlQuery: string;

  get displayedQuery(): string {
    return trimPreludeFromQuery(this.taxiQlQuery);

  }
}

export function trimPreludeFromQuery(query: string): string {
  return trimLinesStartingWith(query, ['import', '@', 'query']);
}


export function trimLinesStartingWith(text: string, prefixes: string[]) {
  return text.split("\n")
    .filter(line => !prefixes.some(prefix => line.startsWith(prefix)))
    .map(line => line.trim())
    .join("\n")
}
