import {
  computed,
  effect,
  Injectable,
  Injector,
  runInInjectionContext,
  Signal,
  signal,
  untracked,
  WritableSignal
} from '@angular/core';
import {isNullOrUndefined} from '../utils/utils';
import {QueryEditorStoreService} from './query-editor-store.service';
import {
  dangerouslyConvertToSavedQueryWithSource,
  SavedQueryWithSource
} from '../project-import/schema-importer.service';
import {ConversationMessage} from './query.service';
import {SavedQuery} from "./types.service";

export type LocalStorageQuery = {
  id: number,
  tabName: string,
  isActive: boolean,
  savedQueryWithSource: SavedQueryWithSource,
  query: string,
  conversationMessages: ConversationMessage[],
}

const PERSISTED_QUERIES_LOCAL_STORAGE_KEY: string = 'persistedQueries'
const DEPRECATED_QUERY_LOCAL_STORAGE_KEY: string = 'persistedQuery'

@Injectable({
  providedIn: 'root'
})
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
    const sanitisedQueries = this.checkForDeprecatedProps(persistedQueries)
    if (sanitisedQueries) {
      this.queries.set(sanitisedQueries);
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
    effect(() => {
      const messages = this.editorStore.activeQueryEditorState().payload.conversationMessages()
      untracked(() => {
        if (messages.length > 0 && this.activeQuery().conversationMessages.length !== messages.length) {
          this.updateConversationMessages(messages);
        }
      })
    })
  }

  onTabIndexChanged(index: number) {
    const clonedQueries = this.queries().slice();
    // reset the previous index isActive to be false
    clonedQueries.map(query => query.isActive = false);
    clonedQueries[index].isActive = true;
    this.queries.set(clonedQueries);
    this.editorStore.updateActiveQueryEditorStateIndex(index)
  }

  addTab(title: string = '', query: string = '', savedQueryWithSource?: SavedQueryWithSource) {
    const newTab: LocalStorageQuery = {
      id: Date.now(), // TODO: use the same randomId() function as queryClientId?
      tabName: title || this.generateTabName(),
      isActive: true,
      savedQueryWithSource,
      query,
      conversationMessages: [],
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

  updateQuery(query: string, append?: boolean) {
    const updatedQuery = append ? query + '\n' + this.activeQuery().query : query
    this.patchQuery('query', updatedQuery)
    this.editorStore.updateQuery(updatedQuery)
  }

  onQuerySaved($event: SavedQueryWithSource) {
    const clonedQueries = this.queries().slice();
    clonedQueries[this.activeTabIndex()].tabName = $event.savedQuery.name.shortDisplayName;
    clonedQueries[this.activeTabIndex()].savedQueryWithSource = $event;
    this.queries.set(clonedQueries);
  }

  onSavedQuerySelected($event: SavedQuery) {
    const savedQueryWithSource:SavedQueryWithSource = dangerouslyConvertToSavedQueryWithSource($event)
    this.addTab($event.name.name, $event.sources[0].content, savedQueryWithSource)
  }

  deleteChatHistory() {
    this.updateConversationMessages([]);
    this.editorStore.resetConversationMessages()
  }

  private updateConversationMessages(conversationMessages: ConversationMessage[]) {
    this.patchQuery('conversationMessages', conversationMessages)
  }

  private updateLocalStorage() {
    localStorage.setItem(PERSISTED_QUERIES_LOCAL_STORAGE_KEY, JSON.stringify(this.queries()));
  }

  private checkForDeprecatedLocalStorage() {
    const deprecatedQuery = localStorage.getItem(DEPRECATED_QUERY_LOCAL_STORAGE_KEY);
    if (deprecatedQuery) {
      this.addTab('Untitled', deprecatedQuery);
      localStorage.removeItem(DEPRECATED_QUERY_LOCAL_STORAGE_KEY)
    }
  }

  private checkForDeprecatedProps(queries: LocalStorageQuery[]): LocalStorageQuery[] {
    return queries.map(query => {
      if (isNullOrUndefined(query.conversationMessages)) {
        query.conversationMessages = []
      } else if (!isNullOrUndefined(query['chatQuery'])) {
        this.editorStore.activeQueryEditorState().payload.lastChatGptText.set(query['chatQuery'])
        delete query['chatQuery']
      }
      return query
    })
  }

  private patchQuery<K extends keyof LocalStorageQuery>(key: K, value: LocalStorageQuery[K]) {
    const updatedQuery: LocalStorageQuery = {
      ...this.activeQuery(),
      [key]: value
    }
    const clonedQueries = this.queries().slice();
    clonedQueries[this.activeTabIndex()] = updatedQuery;
    this.queries.set(clonedQueries);
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
