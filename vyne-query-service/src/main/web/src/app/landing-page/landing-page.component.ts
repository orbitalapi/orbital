import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';
import { QueryHistorySummary } from '../services/query.service';
import { Schema } from '../services/schema';
import { ChangeLogEntry } from '../changelog/changelog.service';

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
    <app-search-bar-container></app-search-bar-container>
    <div class="container">
      <div class="content-box schema-diagram-container">
        <h3>Your services</h3>
        <app-schema-diagram [schema]="schema" [displayedMembers]="displayedSchemaMembers"></app-schema-diagram>
      </div>
      <div class="content-box changelog-container">
        <h3>Changelog</h3>
        <app-changelog-list [changeLogEntries]="changeLogEntries"></app-changelog-list>
      </div>


    </div>


  `
})
export class LandingPageComponent {
  constructor(public readonly router: Router) {
  }

  @Input()
  changeLogEntries: ChangeLogEntry[];

  // Names of types / services
  displayedSchemaMembers: string[] = [];

  private _schema: Schema;

  @Input()
  get schema(): Schema {
    return this._schema;
  }

  set schema(value: Schema) {
    if (value === this.schema) {
      return;
    }
    this._schema = value;
    this.resetDisplayedMembers(this.schema)
  }

  dataSources: any[] = [];
  recentQueries: QueryHistorySummary[] = [];

  recentQueryCardConfig = RECENT_QUERIES;
  dataSourcesCardConfig = DATA_SOURCES;
  catalogCardConfig = DATA_CATALOG;

  private resetDisplayedMembers(schema: Schema) {
    this.displayedSchemaMembers = (schema) ? schema.services.map(s => s.name.fullyQualifiedName) : [];
  }
}

export const RECENT_QUERIES: LandingPageCardConfig = {
  title: 'Query',
  emptyText: `Run queries to link data from across all the registered data sources.  Get started by running your first query.`,
  emptyActionLabel: 'Create a query',
  emptyStateImage: 'assets/img/illustrations/search-engine.svg'
}

export const DATA_SOURCES: LandingPageCardConfig = {
  title: 'Sources',
  emptyText: `Data sources and schemas define the places Vyne can fetch data.  Add a data source to get started.`,
  emptyActionLabel: 'Add a data source',
  emptyStateImage: 'assets/img/illustrations/data-settings.svg'
}

export const DATA_CATALOG: LandingPageCardConfig = {
  title: 'Catalog',
  emptyText: 'A one-stop searchable catalog of all your glossary items, data models, sources and APIs registered with Vyne.',
  emptyActionLabel: 'Search the catalog',
  emptyStateImage: 'assets/img/illustrations/catalog.svg'
}
