import {Component, Input, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {QueryHistorySummary} from '../services/query.service';

export interface LandingPageCardConfig {
  title: string;
  emptyText: string;
  emptyActionLabel: string;
  emptyStateImage: string;
}

@Component({
  selector: 'app-landing-page',
  styleUrls: ['./landing-page.component.scss'],
  template: `
    <app-header-bar title="">
    </app-header-bar>
    <div class="page-content">
      <div class="row search-row">
        <app-landing-card [cardConfig]="catalogCardConfig" [isEmpty]="true" layout="horizontal"></app-landing-card>
      </div>
      <div class="row card-row">
        <app-landing-card [cardConfig]="recentQueryCardConfig" [isEmpty]="recentQueries.length === 0" layout="vertical"
                          (emptyActionClicked)="router.navigate(['query-wizard'])"></app-landing-card>
        <app-landing-card [cardConfig]="dataSourcesCardConfig" [isEmpty]="dataSources.length === 0" layout="vertical"
                          (emptyActionClicked)="router.navigate(['schema-explorer','import'])"></app-landing-card>
      </div>
    </div>
  `
})
export class LandingPageComponent {

  constructor(public readonly router: Router) {
  }

  dataSources: any[] = [];
  recentQueries: QueryHistorySummary[] = [];

  recentQueryCardConfig = RECENT_QUERIES;
  dataSourcesCardConfig = DATA_SOURCES;
  catalogCardConfig = DATA_CATALOG;

}

export const RECENT_QUERIES: LandingPageCardConfig = {
  title: 'Recent queries',
  emptyText: `Queries your team have run will appear here.  Get started by running your first query.`,
  emptyActionLabel: 'Create a query',
  emptyStateImage: 'assets/img/illustrations/search-engine.svg'
}

export const DATA_SOURCES: LandingPageCardConfig = {
  title: 'Sources',
  emptyText: `Data sources and schemas will show here once registered.  Add a data source to get started.`,
  emptyActionLabel: 'Add a data source',
  emptyStateImage: 'assets/img/illustrations/data-settings.svg'
}

export const DATA_CATALOG: LandingPageCardConfig = {
  title: 'Catalog',
  emptyText: 'A one-stop searchable catalog of all your glossary items, data models, sources and APIs registered with Vyne.',
  emptyActionLabel: 'Search the catalog',
  emptyStateImage: 'assets/img/illustrations/catalog.svg'
}
