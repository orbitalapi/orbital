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
    return trimImportsFromQuery(this.taxiQlQuery);

  }
}

export function trimImportsFromQuery(query: string): string {
  return query.split("\n")
    .filter(line => !line.startsWith("import"))
    .map(line => line.trim())
    .join("\n")
}
