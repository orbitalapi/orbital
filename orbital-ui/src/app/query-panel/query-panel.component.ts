import {ChangeDetectionStrategy, Component, OnDestroy} from '@angular/core';
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
export class QueryPanelComponent implements OnDestroy {

  constructor(
    protected queryPanelStore: QueryPanelStoreService,
    protected editorStore: QueryEditorStoreService
  ) {
    this.editorStore.isQueryEditorActive = true;
  }

  protected readonly JSON = JSON;

  ngOnDestroy(): void {
    this.editorStore.isQueryEditorActive = false;
  }
}
