import { ChangeDetectionStrategy, Component } from '@angular/core';
import { QueryEditorStoreService } from '../services/query-editor-store.service';
import { QueryPanelStoreService } from '../services/query-panel-store.service';

@Component({
  selector: 'app-query-panel',
  templateUrl: './query-panel.component.html',
  styleUrls: ['./query-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  // Note: these Services live and die with the QueryPanelComponent
  //providers: [QueryPanelStoreService, QueryEditorStoreService]
})
export class QueryPanelComponent {

  constructor(
    protected queryPanelStore: QueryPanelStoreService,
    protected editorStore: QueryEditorStoreService
  ) {
  }

  protected readonly JSON = JSON;
}
