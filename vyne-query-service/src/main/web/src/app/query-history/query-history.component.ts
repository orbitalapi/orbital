import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {
  isRestQueryHistoryRecord,
  isVyneQlQueryHistoryRecord,
  ProfilerOperation,
  QueryHistoryRecord, QueryHistorySummary,
  QueryService,
  VyneQlQueryHistoryRecord,
} from '../services/query.service';
import {isStyleUrlResolvable} from '@angular/compiler/src/style_url_resolver';
import {InstanceSelectedEvent} from '../query-panel/result-display/result-container.component';
import {InstanceLike} from '../object-view/object-view.component';
import {Type} from '../services/schema';
import {ActivatedRoute, Router} from '@angular/router';


@Component({
  selector: 'app-query-history',
  templateUrl: './query-history.component.html',
  styleUrls: ['./query-history.component.scss']
})
export class QueryHistoryComponent implements OnInit {
  history: QueryHistorySummary[];
  activeRecord: QueryHistoryRecord;

  @Output() hasTypedInstanceDrawerClosed = new EventEmitter<boolean>();
  shouldTypedInstancePanelBeVisible: boolean;

  constructor(private service: QueryService, private router: Router) {
  }

  profileLoading = false;
  profilerOperation: ProfilerOperation;
  private _queryResponseId: string;


  @Input()
  get queryResponseId(): string {
    return this._queryResponseId;
  }

  set queryResponseId(value: string) {
    this._queryResponseId = value;
  }


  selectedTypeInstance: InstanceLike;
  selectedTypeInstanceType: Type;

  get showSidePanel(): boolean {
    return this.selectedTypeInstanceType !== undefined && this.selectedTypeInstance !== null;
  }

  set showSidePanel(value: boolean) {
    if (!value) {
      this.selectedTypeInstance = null;
    }
  }


  ngOnInit() {
    this.loadData();
    if (this._queryResponseId && this._queryResponseId.length > 0) {
      this.setActiveRecordFromRoute();
    }
  }

  loadData() {
    this.service.getHistory()
      .subscribe(history => this.history = history);
  }

  typeName(qualifiedTypeName: string) {
    // TODO : There's a correct parser for type names on the server
    // which can handle generics.
    // Consider using that, instead of this dirty hack

    if (qualifiedTypeName.startsWith('lang.taxi.Array<')) {
      const collectionMemberName = qualifiedTypeName.replace('lang.taxi.Array<', '').slice(0, -1);
      return this.typeName(collectionMemberName) + '[]';
    } else {
      const parts = qualifiedTypeName.split('.');
      return parts[parts.length - 1];
    }
  }

  isVyneQlQuery(record: QueryHistoryRecord): boolean {
    return isVyneQlQueryHistoryRecord(record);
  }

  isRestQuery(record: QueryHistoryRecord): boolean {
    return isRestQueryHistoryRecord(record);
  }

  setActiveRecord(historyRecord: QueryHistorySummary) {
    this.profilerOperation = null;
    this.profileLoading = true;
    this.service.getHistoryRecord(historyRecord.queryId).subscribe(
      result => {
        this.activeRecord = result;
      }
    );
    this.service.getQueryProfile(historyRecord.queryId).subscribe(
      result => {
        this.profileLoading = false;
        this.profilerOperation = result;
      }
    );
    this.setRouteFromActiveRecord();
  }

  setActiveRecordFromRoute() {
    this.service.getHistoryRecord(this._queryResponseId)
      .subscribe(record => {
        this.activeRecord = record;
      });
  }

  setRouteFromActiveRecord() {
    this.router.navigate(['/query-history', this.activeRecord.id]);
  }

  onInstanceSelected($event: InstanceSelectedEvent) {
    this.shouldTypedInstancePanelBeVisible = true;
    this.selectedTypeInstance = $event.selectedTypeInstance;
    this.selectedTypeInstanceType = $event.selectedTypeInstanceType;
  }

  onCloseTypedInstanceDrawer($event: boolean) {
    this.shouldTypedInstancePanelBeVisible = $event;
  }
}

