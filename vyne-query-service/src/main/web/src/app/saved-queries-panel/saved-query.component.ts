import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {SavedQuery} from "../services/type-editor.service";
import {trimPreludeFromQuery} from "../query-history/vyneql-record.component";

@Component({
  selector: 'app-saved-query',
  template: `
    <div class="row">
      <h4>{{ query.name.shortDisplayName }}</h4>
    </div>
    <div class="row">
      <div class="query-code">{{ taxiQl  | truncate: 100  }}</div>
    </div>
  `,
  styleUrls: ['./saved-query.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SavedQueryComponent {

  @Input()
  query: SavedQuery

  get taxiQl(): string {
    if (!this.query) return '';
    return this.query.sources.map(s => {
      return trimPreludeFromQuery(s.content);
    }).join('\n');
  }
}
