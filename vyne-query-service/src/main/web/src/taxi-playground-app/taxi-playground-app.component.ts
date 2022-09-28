import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ParsedSchema, TaxiPlaygroundService } from 'src/taxi-playground-app/taxi-playground.service';
import { debounceTime, filter, map, share, switchMap} from 'rxjs/operators';
import { emptySchema, Schema } from 'src/app/services/schema';
import { Observable, of, Subject } from 'rxjs';

const intro = `/*
Welcome to Voyager - a microservices diagramming tool.

Easily create diagrams that visualize the connections between services and data sources in your stack

How it works:

- Describe your services and model

That's it! Links between servies will automatically be made where types are shared.

e.g. In the example below, we have a Reviews service which accepts an Input of FilmId and returns a FilmReview.

Also described is the FilmReview model, and we can see that a connection has been added between the two objects in the diagram.

We'd love to hear what you think. Head over to our GitHub repo to report issues, or jump on our Slack channel to chat.
*/

model Film {
  filmId : FilmId inherits String
}

service FilmsDatabase {
  table films : Film[]
}

model FilmReview {
  id : FilmId
  reviewScore: Int
}

service Reviews {
  operation getReview(FilmId): FilmReview
}
`;

@Component({
  selector: 'taxi-playground-app',
  template: `
    <playground-toolbar></playground-toolbar>
    <div class="container">
      <as-split direction="horizontal" unit="percent">
        <as-split-area [size]="35">
          <app-code-editor 
            [content]="intro" 
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

  codeUpdated$ = new Subject<string>()

  schema$: Observable<Schema>;

  fullscreen = false;

  intro = intro;

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
        share()
      )
  }

  onFullscreenChange() {
    this.fullscreen = !this.fullscreen; 
  }
}
