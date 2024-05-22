import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, Injector} from '@angular/core';
import {ParsedSchema, VoyagerService} from 'src/voyager-app/voyager.service';
import {catchError, debounceTime, filter, map, mergeMap, shareReplay, switchMap, take, tap} from 'rxjs/operators';
import {emptySchema, Schema} from 'src/app/services/schema';
import {Observable, of, ReplaySubject} from 'rxjs';
import {ExampleGroups, StubExamples} from 'src/voyager-app/code-examples';
import {TuiAlertService, TuiDialogService} from '@taiga-ui/core';
import {ShareDialogComponent} from 'src/app/voyager/share-dialog/share-dialog.component';
import {PolymorpheusComponent} from '@tinkoff/ng-polymorpheus';
import {ActivatedRoute, Params} from '@angular/router';
import {emptyQueryMessage, StubQueryMessage, StubQueryMessageWithSlug} from "../app/services/query.service";
import {isNullOrUndefined} from "../app/utils/utils";

@Component({
  selector: 'voyager-app',
  template: `
    <div class="app-container">
      <playground-toolbar (selectedExampleChange)="setCodeFromExample($event.query)"
                          (generateShareUrl)="showShareDialog()"
                          (clear)="clear()"
      ></playground-toolbar>
      <div class="container">
        <app-voyager-sidebar [(showDiagram)]="showDiagram" [(showQueryPanel)]="showQueryPanel" (copyDevCode)="copyDevCode()"/>
        <as-split direction="horizontal" unit="percent" gutterSize="1">
          <div class="thin-splitter" *asSplitGutter="let isDragged = isDragged" [class.dragged]="isDragged">
            <div class="thin-splitter-gutter-icon"></div>
          </div>
          <as-split-area [size]="35" [order]="0">
            <div class="panel-with-header">
              <app-panel-header title="Schema"></app-panel-header>
              <as-split direction="vertical" unit="pixel">
                <as-split-area size="*">
                  <app-code-editor
                    class="flex-grow"
                    [content]="content"
                    wordWrap="on"
                    (contentChange)="codeUpdated$.next($event)">
                  </app-code-editor>
                </as-split-area>
                <as-split-area [size]="200" *ngIf="(parsedSchema$ | async)?.hasErrors">
                  <app-compilation-message-list [compilationMessages]="(parsedSchema$ | async).messages"></app-compilation-message-list>
                </as-split-area>
              </as-split>
            </div>
          </as-split-area>
          <as-split-area *ngIf="showQueryPanel" [order]="1">
            <app-playground-query-panel [schema]="schema$ | async"
                                        [queryMessage]="queryMessage"></app-playground-query-panel>
          </as-split-area>
          <as-split-area *ngIf="showDiagram" [order]="2">
            <div class="panel-with-header">
              <app-panel-header title="Diagram"></app-panel-header>
              <app-schema-diagram
                class="flex-grow"
                [class.mat-elevation-z8]="fullscreen"
                [class.fullscreen]="fullscreen"
                [schema$]="schema$"
                displayedMembers="everything"
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
export class VoyagerAppComponent {

  showDiagram: boolean = true;
  showQueryPanel: boolean = true;

  queryMessage: StubQueryMessage;

  codeUpdated$ = new ReplaySubject<string>(1)

  schema$: Observable<Schema>;
  parsedSchema$: Observable<ParsedSchema>

  fullscreen = false;

  content: string | null = null;

  constructor(private service: VoyagerService,
              @Inject(TuiDialogService) private readonly dialogService: TuiDialogService,
              @Inject(Injector) private readonly injector: Injector,
              private readonly activatedRoute: ActivatedRoute,
              private readonly changeDetectorRef: ChangeDetectorRef,
              private readonly alertsService: TuiAlertService
  ) {
    this.parsedSchema$ = this.codeUpdated$
      .pipe(
        debounceTime(250),
        tap(value => {
          this.content = value;
          this.changeDetectorRef.markForCheck();
        }),
        switchMap((source: string) => {
          if (source && source.length > 0) {
            this.queryMessage.schema = source;
            return this.service.parse(source)
              .pipe(
                catchError((error) => {
                  console.error('Error parsing source: ', error);
                  this.alertsService.open('The compiler threw an exception compiling your source - this shouldn\'t happen)',
                    {
                      status: "error",
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
              schema: emptySchema()
            } as ParsedSchema)
          }
        }),
        // Sharing is caring.  If we don't do this, then we end up with
        // a service call for every subscriber. :(
        shareReplay(1)
      );

    this.schema$ = this.parsedSchema$
      .pipe(
        filter(parseResult => {
          return !parseResult.hasErrors;
        }),
        map(parseResult => {
          return parseResult.schema;
        }),
      );
    this.setCodeFromExample(StubExamples[0].query);

    this.activatedRoute.params
      .pipe(
        filter(params => params['shareSlug'] !== undefined || params['exampleSlug'] !== undefined),
        mergeMap((params: Params) => {
          const shareSlug = params['shareSlug'];
          const exampleSlug = params['exampleSlug'];

          if (shareSlug) {
            return this.service.loadSharedSchema(params['shareSlug'])
          } else {
            const query = ExampleGroups.flatMap(group => group.snippets)
              .find(example => example.slug === exampleSlug)
              .query
            return of(query);
          }

        })
      )
      .subscribe(stubQueryMessage => {
        this.setCodeFromExample(stubQueryMessage)
      });
  }


  onFullscreenChange() {
    this.fullscreen = !this.fullscreen;
  }

  setCodeFromExample(queryMessage: StubQueryMessage) {
    this.queryMessage = queryMessage;
    this.setCode(queryMessage.schema)
    this.showQueryPanel = !isNullOrUndefined(queryMessage.query) && queryMessage.query.length > 0;
  }



  clear() {
    this.setCodeFromExample(emptyQueryMessage())
  }

  setCode(code: string) {
    this.content = code;
    this.codeUpdated$.next(code);
  }

  showShareDialog() {
    this.codeUpdated$.pipe(
      take(1),
      mergeMap((source: string) => {
        return this.service.getShareUrl(source)
      }),
    ).subscribe(result => {
      this.dialogService.open(
        new PolymorpheusComponent(ShareDialogComponent, this.injector), {
          data: result
        }
      ).subscribe()
    })

  }

  copyDevCode() {
    console.log(this.queryMessage)
  }
}
