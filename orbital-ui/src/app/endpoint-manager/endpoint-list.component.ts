import {ChangeDetectionStrategy, Component} from '@angular/core';
import {Observable} from 'rxjs';
import {TypesService} from "../services/types.service";
import {SavedQuery} from "../services/type-editor.service";
import {ActivatedRoute, Router} from "@angular/router";

@Component({
  selector: 'app-endpoint-list',
  template: `
    <app-header-component-layout title="Query Endpoints"
                                 description="Queries, Streams and Pipelines defined in your schema">
      <div *ngIf="queries$ | async as queries">
        <table class="query-list">
          <thead>
          <tr>
            <th>Name</th>
            <th>Type</th>
            <th>URL</th>
          </tr>
          </thead>
          <tbody>
          <tr *ngFor="let query of queries" (click)="navigateToQueryPage(query)">
            <td>{{query.name.shortDisplayName}}</td>
            <td>{{query.queryKind}}</td>
            <td>
              <div *ngIf="query.httpEndpoint" class="url-parts">
                <span class="method">{{query.httpEndpoint.method}}</span>
                <span class="url">{{query.httpEndpoint.url}}</span>
              </div>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

    </app-header-component-layout>
  `,
  styleUrls: ['./endpoint-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EndpointListComponent {

  activeTabIndex = 0;
  queries$: Observable<SavedQuery[]>;



  constructor(typeService: TypesService, private router: Router, private activeRoute: ActivatedRoute) {
    this.queries$ = typeService.getQueries()
  }

  navigateToQueryPage(query: SavedQuery) {
    this.router.navigate([query.name.parameterizedName], {relativeTo: this.activeRoute})
  }
}
