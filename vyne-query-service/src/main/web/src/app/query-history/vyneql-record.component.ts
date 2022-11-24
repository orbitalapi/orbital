import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
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

  get displayedQuery():string {
    return this.taxiQlQuery.split("\n")
      .filter(line => !line.startsWith("import"))
      .join("\n")

  }
}
