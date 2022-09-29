import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ParsedSchema, TaxiPlaygroundService } from 'src/taxi-playground-app/taxi-playground.service';
import { debounceTime, filter, map, share, shareReplay, switchMap } from 'rxjs/operators';
import { emptySchema, Schema } from 'src/app/services/schema';
import { Observable, of, Subject, defer, ReplaySubject } from 'rxjs';
import { CodeSamples } from 'src/taxi-playground-app/code-examples';

@Component({
  selector: 'taxi-playground-app',
  template: `
    <playground-toolbar></playground-toolbar>
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
  `,
  styleUrls: ['./taxi-playground-app.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TaxiPlaygroundAppComponent {

  codeUpdated$ = new ReplaySubject<string>(1)

  schema$: Observable<Schema>;

  fullscreen = false;

  content: string | null = null;

  constructor(private service: TaxiPlaygroundService) {
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
    this.setCodeSample(0);

  }

  setCodeSample(index: number) {
    this.content = CodeSamples[index].code
    this.codeUpdated$.next(this.content);
  }

  onFullscreenChange() {
    this.fullscreen = !this.fullscreen;
  }
}
