import { ChangeDetectionStrategy, Component, Inject, Injector } from '@angular/core';
import {
  ParsedSchema,
  SharedSchemaResponse,
  TaxiPlaygroundService
} from 'src/taxi-playground-app/taxi-playground.service';
import { debounceTime, filter, map, mergeMap, share, shareReplay, switchMap, take } from 'rxjs/operators';
import { emptySchema, Schema } from 'src/app/services/schema';
import { Observable, of, Subject, defer, ReplaySubject } from 'rxjs';
import { CodeSample, CodeSamples } from 'src/taxi-playground-app/code-examples';
import { TuiDialogService } from '@taiga-ui/core';
import { ShareDialogComponent } from 'src/app/taxi-playground/share-dialog/share-dialog.component';
import { PolymorpheusComponent } from '@tinkoff/ng-polymorpheus';
import { ActivatedRoute } from '@angular/router';

/**
 * A simple wrapper, to allow injection of the router, so we can access share urls etc.
 */
@Component({
  selector: 'taxi-playground-container-app',
  template: `
    <tui-root>
      <router-outlet></router-outlet>
    </tui-root>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TaxiPlaygroundContainerAppComponent {

}
