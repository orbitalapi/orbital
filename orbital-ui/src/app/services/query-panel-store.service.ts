import {
  computed,
  effect,
  Injectable,
  Injector,
  runInInjectionContext,
  Signal,
  signal,
  WritableSignal
} from '@angular/core';
import {QueryLanguage} from '../query-panel/query-editor/query-editor-toolbar.component';
import {QueryEditorStoreService} from './query-editor-store.service';
import {
  dangerouslyConvertToSavedQueryWithSource,
  SavedQueryWithSource
} from '../project-import/schema-importer.service';
import {SavedQuery} from "./types.service";

export type LocalStorageQuery = {
  id: number,
  tabName: string,
  isActive: boolean,
  savedQueryWithSource: SavedQueryWithSource,
  // isPublished: boolean, // TODO: implement this
  query: string,
  chatQuery: string,
  queryLanguage: QueryLanguage
}

const PERSISTED_QUERIES_LOCAL_STORAGE_KEY: string = 'persistedQueries'
const DEPRECATED_QUERY_LOCAL_STORAGE_KEY: string = 'persistedQuery'

@Injectable()
export class QueryPanelStoreService {
  readonly queries: WritableSignal<LocalStorageQuery[]> = signal([]);
  readonly activeQuery: Signal<LocalStorageQuery> = computed(() => this.queries()[this.activeTabIndex()])
  readonly activeTabIndex: Signal<number> = computed(() => {
    return Math.max(this.queries().findIndex(query => query.isActive), 0)
  })
  readonly getTabId = (index: number, item: LocalStorageQuery): string => item.id.toString();

  constructor(
    private editorStore: QueryEditorStoreService,
    private injector: Injector
  ) {
    this.checkForDeprecatedLocalStorage();
    const persistedQueries = JSON.parse(localStorage.getItem(PERSISTED_QUERIES_LOCAL_STORAGE_KEY));
    if (persistedQueries) {
      this.queries.set(persistedQueries);
      this.queries().map(query => this.editorStore.addQueryEditorState(query))
      this.onTabIndexChanged(this.activeTabIndex())
    } else {
      this.addTab()
    }
    effect(() => {
      if (this.queries()) {
        this.updateLocalStorage();
      }
    })
  }

  onTabIndexChanged(index: number) {
    const clonedQueries = this.queries().slice();
    // reset the previous index isActive to be false
    clonedQueries.map(query => query.isActive = false);
    clonedQueries[index].isActive = true;
    this.queries.set(clonedQueries);
    this.editorStore.activeQueryEditorState.set(this.editorStore.queryEditorStates()[index])
  }

  addTab(title: string = '', query: string = '', chatQuery: string = '', savedQueryWithSource?: SavedQueryWithSource) {
    const newTab: LocalStorageQuery = {
      id: Date.now(), // TODO: use the same randomId() function as queryClientId?
      tabName: title || this.generateTabName(),
      isActive: true,
      savedQueryWithSource,
      query,
      chatQuery,
      queryLanguage: 'TaxiQL'
    }
    runInInjectionContext(this.injector, () => {
      this.editorStore.addQueryEditorState(newTab)
    })
    this.queries.update(queries => [...queries, newTab])
    this.onTabIndexChanged(this.queries().length - 1)
  }

  closeTab(event: MouseEvent, index: number) {
    event.stopPropagation();
    const clonedQueries = this.queries().slice();
    clonedQueries.splice(index, 1)
    const previousActiveTabIndex = this.activeTabIndex();
    this.queries.set(clonedQueries);
    this.editorStore.removeQueryEditorState(index)
    if (index <= previousActiveTabIndex) {
      this.onTabIndexChanged(Math.max(previousActiveTabIndex - 1, 0));
    }
  }

  updateQuery(query: string, chatQuery: string) {
    const updatedQuery: LocalStorageQuery = {
      id: this.activeQuery().id,
      tabName: this.activeQuery().tabName,
      isActive: this.activeQuery().isActive,
      savedQueryWithSource: this.activeQuery().savedQueryWithSource,
      query,
      chatQuery,
      queryLanguage: this.activeQuery().queryLanguage
    }
    const clonedQueries = this.queries().slice();
    clonedQueries[this.activeTabIndex()] = updatedQuery;
    this.queries.set(clonedQueries);
    this.editorStore.updateQuery(query, chatQuery)
  }

  updateQueryLanguage($event: QueryLanguage) {
    const clonedQueries = this.queries().slice();
    clonedQueries[this.activeTabIndex()].queryLanguage = $event
    this.queries.set(clonedQueries)
    this.editorStore.updateQueryLanguage($event)
  }

  onQuerySaved($event: SavedQueryWithSource) {
    const clonedQueries = this.queries().slice();
    clonedQueries[this.activeTabIndex()].tabName = $event.savedQuery.name.shortDisplayName;
    clonedQueries[this.activeTabIndex()].savedQueryWithSource = $event;
    this.queries.set(clonedQueries);
  }

  onSavedQuerySelected($event: SavedQuery) {
    const savedQueryWithSource:SavedQueryWithSource = dangerouslyConvertToSavedQueryWithSource($event)

    this.addTab($event.name.name, $event.sources[0].content, null, savedQueryWithSource)
  }

  private updateLocalStorage() {
    localStorage.setItem(PERSISTED_QUERIES_LOCAL_STORAGE_KEY, JSON.stringify(this.queries()));
  }

  private checkForDeprecatedLocalStorage() {
    const deprecatedQuery = localStorage.getItem(DEPRECATED_QUERY_LOCAL_STORAGE_KEY);
    if (deprecatedQuery) {
      this.addTab('Untitled', deprecatedQuery, null);
      localStorage.removeItem(DEPRECATED_QUERY_LOCAL_STORAGE_KEY)
    }
  }

  private generateTabName(): string {
    // Find the highest numeric suffix in existing labels
    const highestSuffix = this.queries().reduce((maxSuffix, obj) => {
      const match = obj.tabName.match(/Untitled (\d+)/);
      if (match) {
        const suffix = parseInt(match[1]);
        return Math.max(maxSuffix, suffix);
      }
      return maxSuffix;
    }, 0);
    return `Untitled ${highestSuffix + 1}`;
  }

}
