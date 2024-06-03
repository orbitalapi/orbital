import {Clipboard} from '@angular/cdk/clipboard';
import {CommonModule} from '@angular/common';
import {ChangeDetectionStrategy, Component, Inject, Input, OnChanges, SimpleChanges} from '@angular/core';
import {RouterLink} from '@angular/router';
import {TuiAlertService, TuiDataListModule, TuiHintModule, TuiNotification} from '@taiga-ui/core';
import {HttpMethod} from '../../../project-import/schema-importer.service';
import {SavedQuery} from '../../../services/types.service';
import {DropdownComponent} from './dropdown/dropdown.component';

type QueryPayload = {
  queryName: string,
  path: string,
  method?: HttpMethod
}

@Component({
  selector: 'app-published-endpoint-info',
  standalone: true,
  styleUrl: './published-endpoint-info.component.scss',
  imports: [CommonModule, RouterLink, DropdownComponent, TuiDataListModule, TuiHintModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div *ngFor="let query of queries; index as i; trackBy: queryName" class="published-endpoint">
      <a
        *ngIf="showTitle && i === 0"
        [routerLink]="'/endpoints/'+query.queryName"
        class="endpoint-link"
      >
        <img src="assets/img/tabler/broadcast.svg" class="filter-link-color">
        {{query.queryName}}
      </a>
      <span class="mono-badge">{{!query.method ? 'WS' : query.method}}</span>
      <span class="endpoint-path">
        {{query.path}}
        <a *ngIf="query.method === 'GET'" [href]="query.path" target="_blank">
          <img src="assets/img/tabler/external-link.svg" class="filter-link-color">
        </a>
      </span>
      <app-dropdown iconUrl="assets/img/tabler/clipboard.svg" hint="Copy endpoint...">
        <tui-data-list>
          <button tuiOption (click)="copyEndpoint(query, 'URL')">As URL</button>
          <button *ngIf="query.method" tuiOption (click)="copyEndpoint(query, 'cURL')">As cURL statement</button>
        </tui-data-list>
      </app-dropdown>
    </div>
  `,
})
export class PublishedEndpointInfoComponent implements OnChanges {

  @Input()
  savedQuery: SavedQuery | null

  @Input()
  showTitle: boolean

  queries: QueryPayload[] = [];

  queryName(index: number, query: QueryPayload): string {
    return query.queryName;
  }

  constructor(
    private clipboard: Clipboard,
    @Inject(TuiAlertService) private readonly alerts: TuiAlertService,
  ) {
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.queries = []
    if (this.savedQuery?.httpEndpoint) {
      this.queries.push({
        queryName: this.savedQuery.name.shortDisplayName,
        path: this.savedQuery.httpEndpoint.url,
        method: this.savedQuery.httpEndpoint.method,
      })
    }
    if (this.savedQuery?.websocketOperation) {
      this.queries.push({
        queryName: this.savedQuery.name.shortDisplayName,
        path: this.savedQuery.websocketOperation.path,
      })
    }
  }

  copyEndpoint(query: QueryPayload, copyType: 'URL' | 'cURL') {
    const { path, method} = query;
    const absolutePath = window.location.origin + path;
    if (copyType === 'URL') {
      this.clipboard.copy(absolutePath);
    } else if (copyType === 'cURL') {
      let clipboardContent: string;
      switch (method) {
        case 'GET':
          clipboardContent = `curl -X GET "${absolutePath}"`
          break;
        case 'POST':
          clipboardContent = `curl -X POST "${absolutePath}"`
          break
        case 'PUT':
          clipboardContent = `curl -X PUT "${absolutePath}"`
          break
        case 'DELETE':
          clipboardContent = `curl -X DELETE "${absolutePath}"`
          break
      }
      this.clipboard.copy(clipboardContent)
    }
    this.alerts.open('Copied to clipboard', {status: TuiNotification.Success})
      .subscribe()
  }
}

