import {ChangeDetectionStrategy, Component, Inject, Injector} from '@angular/core';
import {ParsedSchema, VoyagerService} from 'src/voyager-app/voyager.service';
import {debounceTime, filter, map, mergeMap, shareReplay, switchMap, take} from 'rxjs/operators';
import {emptySchema, Schema} from 'src/app/services/schema';
import {Observable, of, ReplaySubject} from 'rxjs';
import {CodeSample, CodeSamples} from 'src/voyager-app/code-examples';
import {TuiDialogService} from '@taiga-ui/core';
import {ShareDialogComponent} from 'src/app/voyager/share-dialog/share-dialog.component';
import {PolymorpheusComponent} from '@tinkoff/ng-polymorpheus';
import {ActivatedRoute, Params} from '@angular/router';

@Component({
  selector: 'voyager-app',
  template: `
    <div class="app-container">
      <playground-toolbar (selectedExampleChange)="setCodeFromExample($event)"
                          (generateShareUrl)="showShareDialog()"></playground-toolbar>
      <div class="container">
        <as-split direction="horizontal" unit="percent">
          <as-split-area [size]="35">
            <app-code-editor
              [content]="content"
              wordWrap="on"
              (contentChange)="codeUpdated$.next($event)">
            </app-code-editor>
          </as-split-area>
          <as-split-area>
            <app-schema-diagram
              [class.mat-elevation-z8]="fullscreen"
              [class.fullscreen]="fullscreen"
              [schema$]="schema$"
              displayedMembers="everything"
              (fullscreenChange)="onFullscreenChange()">

            </app-schema-diagram>
          </as-split-area>
        </as-split>
      </div>
    </div>
  `,
  styleUrls: ['./voyager-app.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class VoyagerAppComponent {

  codeUpdated$ = new ReplaySubject<string>(1)

  schema$: Observable<Schema>;

  fullscreen = false;

  content: string | null = null;

  constructor(private service: VoyagerService,
              @Inject(TuiDialogService) private readonly dialogService: TuiDialogService,
              @Inject(Injector) private readonly injector: Injector,
              private readonly activatedRoute: ActivatedRoute
  ) {
    this.schema$ = this.codeUpdated$
      .pipe(
        debounceTime(250),
        switchMap((source: string) => {
          if (source && source.length > 0) {
            return this.service.parse(source)
          } else {
            return of({
              hasErrors: false,
              messages: [],
              schema: emptySchema()
            } as ParsedSchema)
          }

        }),
        filter(parseResult => {
          return !parseResult.hasErrors;
        }),
        map(parseResult => {
          return parseResult.schema;
        }),
        // Sharing is caring.  If we don't do this, then we end up with
        // a service call for every subscriber. :(
        shareReplay(1)
      );
    this.setCodeFromExample(CodeSamples[0]);

    this.activatedRoute.params
      .pipe(
        filter(params => params['shareSlug'] !== undefined),
        mergeMap((params: Params) => {
          return this.service.loadSharedSchema(params['shareSlug'])
        })
      )
      .subscribe(code => {
        this.setCode(code)
      })
  }


  onFullscreenChange() {
    this.fullscreen = !this.fullscreen;
  }

  setCodeFromExample(codeSample: CodeSample) {
    this.setCode(codeSample.code)
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
}
