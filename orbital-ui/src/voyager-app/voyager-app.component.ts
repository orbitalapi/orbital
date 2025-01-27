import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Inject,
  Injector,
  model, OnInit,
  ViewChild,
} from '@angular/core';
import {ParsedSchema, VoyagerService} from 'src/voyager-app/voyager.service';
import {
  catchError,
  debounceTime,
  filter,
  map,
  mergeMap,
  shareReplay,
  switchMap,
  tap,
} from 'rxjs/operators';
import {combineLatest} from 'rxjs'
import {emptySchema, Schema} from 'src/app/services/schema';
import {Observable, of, ReplaySubject} from 'rxjs';
import {ExampleGroups} from 'src/voyager-app/code-examples';
import {TuiAlertService, TuiDialogService} from '@taiga-ui/core';
import {ShareDialogComponent} from 'src/app/voyager/share-dialog/share-dialog.component';
import {PolymorpheusComponent} from '@taiga-ui/polymorpheus';
import {ActivatedRoute, NavigationEnd, Params, Router} from '@angular/router';
import {emptyQueryMessage, StubQueryMessage} from "../app/services/query.service";
import {isNullOrUndefined} from "../app/utils/utils";
import {Clipboard} from '@angular/cdk/clipboard';
import * as pako from 'pako';
import {ReadmePanelComponent} from '../app/voyager/readme-panel/readme-panel.component';
import {PlaygroundQueryPanelComponent} from "../app/voyager/playground-query-panel/playground-query-panel.component";
import {SnippetType} from "../app/voyager/voyager-sidebar/voyager-sidebar.component";
import {PlaygroundSchemaService} from "./playground-schema-service";

@Component({
  selector: 'voyager-app',
  template: `
    <div class="app-container theme-playground">
      <playground-toolbar (selectedExampleChange)="setCodeFromExample($event.query)"
                          (generateShareUrl)="showShareDialog()"
                          (clear)="clear()"
      ></playground-toolbar>
      <div class="container">
        <app-voyager-sidebar
          [showDiagram]="showDiagram"
          (showDiagramChange)="onShowDiagramChanged($event)"
          [showQueryPanel]="showQueryPanel"
          (showQueryPanelChange)="onShowQueryPanelChanged($event)"
          [showReadme]="showReadme"
          (showReadmeChange)="onShowReadmeChanged($event)"
          [showSchema]="showSchema"
          (showSchemaChange)="onShowSchemaChanged($event)"
          (copyDevCode)="copyDevCode($event)"
        />
        <as-split *ngIf="queryMessage" direction="horizontal" unit="percent" gutterSize="1" useTransition="true">
          <div class="thin-splitter" *asSplitGutter="let isDragged = isDragged" [class.dragged]="isDragged">
            <div class="thin-splitter-gutter-icon"></div>
          </div>
          <as-split-area [visible]="!!(showReadme && queryMessage)" [order]="0">
            <app-readme-panel
              [markdown]="queryMessage?.readme"
              (markdownChange)="onReadmeChange($event)"
              (onRunQuery)="onRunQueryHandler($event)"
            ></app-readme-panel>
          </as-split-area>
          <as-split-area [visible]="showSchema" [size]="35" [order]="1">
            <div class="panel-with-header">
              <app-panel-header title="Schema" tablerIcon="code"></app-panel-header>
              <app-code-editor
                class="flex-grow"
                [content]="content"
                wordWrap="on"
                [showCompilationProblemsPanel]="true"
                (contentChange)="codeUpdated$.next($event); onSchemaChanged($event)"
                [setFocus]="false"
              >
              </app-code-editor>
            </div>
          </as-split-area>
          <as-split-area [visible]="showQueryPanel" [order]="2">
            <app-playground-query-panel [schema]="schema$ | async"
                                        [queryMessage]="queryMessage"
                                        (queryMessageChange)="onQueryChanged($event)"
                                        (stubsChanged)="onStubsChanged($event)"
            ></app-playground-query-panel>
          </as-split-area>
          <as-split-area [visible]="showDiagram" [order]="3">
            <div class="panel-with-header">
              <app-panel-header tablerIcon="route-square-2" title="Diagram"></app-panel-header>
              <app-schema-diagram
                class="flex-grow"
                [class.mat-elevation-z8]="fullscreen"
                [class.fullscreen]="fullscreen"
                [schema$]="schema$"
                [(displayedMembers)]="displayedMembers"
                (fullscreenChange)="onFullscreenChange()">
              </app-schema-diagram>
            </div>
          </as-split-area>
        </as-split>
      </div>
    </div>
  `,
  styleUrls: ['./voyager-app.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class VoyagerAppComponent implements OnInit {

  showDiagram: boolean = true;
  showQueryPanel: boolean = true;
  showReadme: boolean = true;
  showSchema: boolean = true;

  queryMessage: StubQueryMessage;

  codeUpdated$ = new ReplaySubject<string>(1)

  schema$: Observable<Schema>;
  parsedSchema$: Observable<ParsedSchema>

  fullscreen = false;

  content: string | null = null;

  displayedMembers = model<string[] | 'everything' | 'services'>('everything')

  private PLAYGROUND_HISTORY_LOCAL_STORAGE_KEY = 'playgroundHistory'

  @ViewChild(PlaygroundQueryPanelComponent) childComponent!: PlaygroundQueryPanelComponent;

  @ViewChild(ReadmePanelComponent)
  readmePanelComponent: ReadmePanelComponent;

  constructor(private voyagerService: VoyagerService,
              private schemaService: PlaygroundSchemaService,
              @Inject(TuiDialogService) private readonly dialogService: TuiDialogService,
              @Inject(Injector) private readonly injector: Injector,
              private readonly activatedRoute: ActivatedRoute,
              private readonly changeDetectorRef: ChangeDetectorRef,
              private readonly alertsService: TuiAlertService,
              private readonly clipboard: Clipboard,
              private readonly router: Router
  ) {
    this.parsedSchema$ = this.codeUpdated$
      .pipe(
        debounceTime(250),
        tap(value => {
          this.content = value;
          this.changeDetectorRef.markForCheck();
        }),
        switchMap((source: string) => {
          const layout = {
            showDiagram: this.showDiagram,
            showReadme: this.showReadme,
            showQuery: this.showQueryPanel,
            showSchema: this.showSchema
          }
          if (source && source.length > 0) {
            this.queryMessage.schema = source;
            this.queryMessage.layout = layout;
            return this.voyagerService.parse(source)
              .pipe(
                catchError((error) => {
                  console.error('Error parsing source: ', error);
                  this.alertsService.open('The compiler threw an exception compiling your source - this shouldn\'t happen)',
                    {
                      appearance: "error",
                      label: 'Something went wrong'
                    }).subscribe()
                  return of({
                    hasErrors: true,
                    messages: [error],
                    schema: emptySchema()
                  })
                })
              )
          } else {
            return of({
              hasErrors: false,
              messages: [],
              schema: emptySchema(),
              layout
            } as ParsedSchema)
          }
        }),
        // Sharing is caring.  If we don't do this, then we end up with
        // a service call for every subscriber. :(
        shareReplay(1)
      );

    this.schema$ = this.parsedSchema$
      .pipe(
        debounceTime(250),
        filter(parseResult => {
          return !parseResult.hasErrors;
        }),
        map(parseResult => {
          // NOTE: reset back to "everything", as the displayedMembers prop is doing
          //       double duties functionality wise within the schema-diagram-component
          if (!parseResult.schema.types.length) {
            this.displayedMembers.set([])
          } else {
            this.displayedMembers.set('everything')
          }
          return parseResult.schema;
        }),
      );

    this.schema$.subscribe(next => {
      console.log('Updating schema');
      this.schemaService.updateSchema(next)
    });

    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        switchMap(() =>
          combineLatest([this.getActiveRouteParams(), this.getActiveRouteFragment()]) // Combine params and fragment
        ),
        mergeMap(([params, fragment]: [Params, string | null]) => {
          const shareSlug = params['shareSlug'];
          const exampleSlug = params['exampleSlug'];

          if (fragment) {
            const queryMessage = this.unzip(fragment.substring(5));
            this.setCodeFromExample(queryMessage);
            return of(null); // Return an empty observable, since fragment is handled directly
          }
          else if (shareSlug) {
            return this.voyagerService.loadSharedSchema(shareSlug);
          } else if (exampleSlug) {
            const query = ExampleGroups.flatMap(group => group.snippets)
              .find(example => example.slug === exampleSlug)?.query;
            return of(query);
          } else {
            return of(emptyQueryMessage());
          }
        })
      )
      .subscribe(stubQueryMessage => {
        if (stubQueryMessage && this.router.url !== '/') {
          this.setCodeFromExample(stubQueryMessage);
        }
      });
  }

  async ngOnInit() {
    // no example route or pako set, so show them the hello world example if they're a new user,
    // (e.g. they DO NOT have a playground history), otherwise rehydrate the pako route from localStorage
    if (this.router.url === '/') {
      const playgroundHistory = localStorage.getItem(this.PLAYGROUND_HISTORY_LOCAL_STORAGE_KEY)
      if (playgroundHistory) {
        await this.router.navigate(['/'], {fragment: playgroundHistory, replaceUrl: true})
      } else {
        await this.router.navigate([`examples/${ExampleGroups[0].snippets[0].slug}`], {replaceUrl: true})
      }
    }
  }

  onFullscreenChange() {
    this.fullscreen = !this.fullscreen;
  }

  setCodeFromExample(queryMessage: StubQueryMessage) {
    this.queryMessage = JSON.parse(JSON.stringify(queryMessage))
    this.setCode(queryMessage.schema)

    this.showQueryPanel = queryMessage.layout?.showQuery ?? (!isNullOrUndefined(queryMessage.query) && queryMessage.query.length > 0);
    this.showReadme = queryMessage.layout?.showReadme ?? (!isNullOrUndefined(queryMessage.readme) && queryMessage.readme.length > 0);

    // We used to honour whatever the user had set before (ie., current state) for these,
    // but it means that if the diagram or schema were turned off currently, and then
    // swapping to an example that doesn't have a query or readme, then the user is left with
    // a blank canvas.
    // So, if we haven't explicitly configured the visibility of these two, default to showing them,
    this.showDiagram = queryMessage.layout?.showDiagram ?? true;
    this.showSchema = queryMessage.layout?.showSchema ?? true;
  }

  async clear() {
    this.codeUpdated$.next('')
    this.setCodeFromExample(emptyQueryMessage())
    this.readmePanelComponent.resetViewMode()
    //await this.router.navigate(['/'])
    this.updateRouteAndPersistTolocalStorage(true)
  }

  setCode(code: string) {
    this.content = code;
    this.codeUpdated$.next(code);
    this.updateRouteAndPersistTolocalStorage()
  }

  onRunQueryHandler($event: string) {
    this.queryMessage = { ...this.queryMessage, query: $event };
    this.changeDetectorRef.detectChanges();
    // this.setCodeFromExample(this.queryMessage)
    this.childComponent.runQuery()
  }

  private unzip(src: string): StubQueryMessage {
    const zipped = Uint8Array.from(atob(src), c => c.charCodeAt(0))
    const decompressed = pako.ungzip(zipped, {to: 'string'})
    const object = JSON.parse(decompressed) as StubQueryMessage
    return object
  }

  private zip(queryMessage: StubQueryMessage): string {
    const deflated = pako.gzip(JSON.stringify(queryMessage));
    const base64Encoded = btoa(String.fromCharCode.apply(null, deflated))
    return base64Encoded;
  }

  showShareDialog() {
   const base64Encoded = this.zip(this.queryMessage);

    const shareUrl = `${window.location.origin}#pako:${base64Encoded}`

    this.dialogService.open(
      new PolymorpheusComponent(ShareDialogComponent, this.injector), {
        data: shareUrl
      }
    ).subscribe()

  }

  copyDevCode(language: SnippetType) {
    if (language === "JSON") {
      this.clipboard.copy(JSON.stringify(this.queryMessage, null, 3))
    } else {
      this.copyAsJavascriptSnippet(this.queryMessage)
    }
  }

  private copyAsJavascriptSnippet(queryMessage: StubQueryMessage) {
    // We do quite a bit of string manipulation to turn the JSON object
    // into a javascript snippet, where long strings like the query and schema
    // have actual new-lines (instead of the string "\n"), and are quoted in backticks.
    const snippet = this.createJavascriptSnippet(queryMessage)

    // Even though the string is now correct, if we copy it to the clipboard as-is,
    // we get the \n output in lines, rather than actual newlines.
    // So, we stick it in a text area, then copy the value from there.
    // Create a temporary textarea element to hold the text
    const textarea = document.createElement('textarea');
    textarea.value = snippet;
    document.body.appendChild(textarea);

    // Select the text in the textarea
    textarea.select();
    textarea.setSelectionRange(0, 99999); // For mobile devices

    // Copy the text to the clipboard
    document.execCommand('copy');

    // Remove the temporary textarea element
    document.body.removeChild(textarea);
  }

  createJavascriptSnippet(queryMessage: StubQueryMessage): string {
    // We do quite a bit of string manipulation to turn the JSON object
    // into a javascript snippet, where long strings like the query and schema
    // have actual new-lines (instead of the string "\n"), and are quoted in backticks.
    const wrapperObject = {
      title: 'Title goes here',
      slug: 'slug-goes-here',
      query: "REPLACEME"
    }

    function asBackTickedString(value: string): string {
      // replace newline strings with actual newLines
      const formattedValue = value.replace(/\\n/g, '\n')
      return "`" + formattedValue + "`"
    }

    let queryAsJson = JSON.stringify({
      ...queryMessage,
      schema: 'SCHEMA_GOES_HERE',
      query: 'QUERY_GOES_HERE'
    }, null, 3)
    queryAsJson = queryAsJson.replace('"SCHEMA_GOES_HERE"', asBackTickedString(queryMessage.schema))
      .replace('"QUERY_GOES_HERE"', asBackTickedString(queryMessage.query))

    const wrapperObjectJson = JSON.stringify(wrapperObject, null, 3)
    const exampleAsJs = wrapperObjectJson.replace('"REPLACEME"', queryAsJson);
    const formattedJsSnippet = `import {StubQueryMessageWithSlug} from "src/app/services/query.service";

export const example: StubQueryMessageWithSlug = ${exampleAsJs}`
    return formattedJsSnippet
  }

  // this is required as without it the activatedRoute only returns the parent route ('/')
  private getActiveRouteParams() {
    let route = this.activatedRoute;

    // Traverse to the deepest activated route (where the params would be)
    while (route.firstChild) {
      route = route.firstChild;
    }

    // Return the observable of the params from the deepest route
    return route.params;
  }

  private getActiveRouteFragment() {
    let route = this.activatedRoute;

    // Traverse to the deepest activated route (where the params would be)
    while (route.firstChild) {
      route = route.firstChild;
    }

    // Return the observable of the params from the deepest route
    return route.fragment;
  }

  onShowDiagramChanged($event: boolean) {
    this.showDiagram = this.queryMessage.layout.showDiagram = $event;
    this.updateRouteAndPersistTolocalStorage()
  }

  onShowQueryPanelChanged($event: boolean) {
    this.showQueryPanel = this.queryMessage.layout.showQuery = $event;
    this.updateRouteAndPersistTolocalStorage()
  }

  onShowReadmeChanged($event: boolean) {
    this.showReadme = this.queryMessage.layout.showReadme = $event;
    this.updateRouteAndPersistTolocalStorage()
  }

  onShowSchemaChanged($event: boolean) {
    this.showSchema = this.queryMessage.layout.showSchema = $event;
    this.updateRouteAndPersistTolocalStorage()
  }

  onReadmeChange($event: string) {
    this.queryMessage.readme = $event
    this.updateRouteAndPersistTolocalStorage()
  }

  onSchemaChanged($event: string) {
    this.queryMessage.schema = $event
    this.updateRouteAndPersistTolocalStorage()
  }

  onQueryChanged($event: string) {
    this.queryMessage.query = $event;
    this.updateRouteAndPersistTolocalStorage()
  }

  onStubsChanged($event: any) {
    this.queryMessage.stubs = $event;
    this.updateRouteAndPersistTolocalStorage()
  }

  private updateRouteAndPersistTolocalStorage(override: boolean = false) {
    const base64Encoded = this.zip(this.queryMessage)
    // if it's the same pako, or we're viewing examples, don't overwrite the URL or persist to localStorage
    if ((!this.router.url.includes(base64Encoded) && !this.router.url.includes('examples')) || override) {
      // NOTE: we replace the URL, unless we're clearing the code out, then we add a new history state,
      // which allows undoing if required by hitting the back button :-)
      this.router.navigate(['/'], {fragment: `pako:${base64Encoded}`, replaceUrl: !override})
      localStorage.setItem(this.PLAYGROUND_HISTORY_LOCAL_STORAGE_KEY, `pako:${base64Encoded}`);
    }
  }
}
