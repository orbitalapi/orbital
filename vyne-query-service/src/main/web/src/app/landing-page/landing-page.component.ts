import {AfterViewInit, Component} from '@angular/core';
import {Router} from '@angular/router';
import {QueryHistorySummary} from '../services/query.service';
import {
  createLanguageClient,
  createTaxiEditor,
  createTaxiEditorModel,
  createUrl,
  createWebsocketConnection,
  performInit
} from "../code-editor/language-server-commons";
import {ITextFileEditorModel} from "@codingame/monaco-vscode-api/monaco";
import {DidOpenTextDocumentNotification} from "vscode-languageclient";
import {MonacoLanguageClient} from "monaco-languageclient";
import {
  IStandaloneCodeEditor
} from "@codingame/monaco-vscode-api/vscode/vs/editor/standalone/browser/standaloneCodeEditor";

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
    <div class='page-content'>
      <h2>Welcome to Orbital</h2>
      <div id="container" class="monaco-editor" style="height: 50vh; width: 100%;"></div>
      <div class='row search-row'>
        <app-landing-card [cardConfig]='catalogCardConfig' [isEmpty]='true' layout='horizontal'
                          (emptyActionClicked)="router.navigate(['catalog'])"></app-landing-card>
      </div>
      <div class='row card-row'>
        <app-landing-card [cardConfig]='recentQueryCardConfig' [isEmpty]='recentQueries.length === 0' layout='vertical'
                          (emptyActionClicked)="router.navigate(['query','editor'])"></app-landing-card>
        <app-landing-card [cardConfig]='dataSourcesCardConfig' [isEmpty]='dataSources.length === 0' layout='vertical'
                          (emptyActionClicked)="router.navigate(['schema-importer'])"></app-landing-card>
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
  title: 'Query',
  emptyText: `Run queries to link data from across all the registered data sources.  Get started by running your first query.`,
  emptyActionLabel: 'Create a query',
  emptyStateImage: 'assets/img/illustrations/search-engine.svg'
};

export const DATA_SOURCES: LandingPageCardConfig = {
  title: 'Sources',
  emptyText: `Data sources and schemas define the places Orbital can fetch data.  Add a data source to get started.`,
  emptyActionLabel: 'Add a data source',
  emptyStateImage: 'assets/img/illustrations/data-settings.svg'
};

export const DATA_CATALOG: LandingPageCardConfig = {
  title: 'Catalog',
  emptyText: 'A one-stop searchable catalog of all your glossary items, data models, sources and APIs registered with Orbital.',
  emptyActionLabel: 'Search the catalog',
  emptyStateImage: 'assets/img/illustrations/catalog.svg'
};
