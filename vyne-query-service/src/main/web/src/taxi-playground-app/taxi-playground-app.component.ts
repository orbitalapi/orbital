import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter } from '@angular/core';
import { TaxiPlaygroundService } from 'src/taxi-playground-app/taxi-playground.service';
import { bufferTime, debounceTime, filter, map, mergeMap, share, switchMap, throttleTime } from 'rxjs/operators';
import { Schema } from 'src/app/services/schema';
import { Observable, Subject } from 'rxjs';
import { AuthService } from '@auth0/auth0-angular';


@Component({
  selector: 'taxi-playground-app',
  template: `
    <playground-toolbar></playground-toolbar>
    <div class="container">
      <as-split direction="horizontal" unit="percent">
        <as-split-area [size]="35">
          <app-code-editor (contentChange)="codeUpdated$.next($event)"></app-code-editor>
        </as-split-area>
        <as-split-area>
          <app-schema-diagram [schema$]="schema$" displayedMembers="everything">

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

  constructor(private service: TaxiPlaygroundService) {
    this.schema$ = this.codeUpdated$
      .pipe(
        debounceTime(250),
        filter(source => source && source.length > 0),
        switchMap((source: string) => {
          console.log('Received updated sources');
          return this.service.parse(source)
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
}
