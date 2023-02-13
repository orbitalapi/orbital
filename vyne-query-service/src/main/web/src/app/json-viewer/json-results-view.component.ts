import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { Observable, Subscription } from 'rxjs';
import { InstanceLike } from 'src/app/services/schema';

@Component({
  selector: 'app-json-results-view',
  template: `
    <app-json-viewer [json]="allInstancesJson" style="height: 100%; width: 100%"></app-json-viewer>
  `,
  styleUrls: ['./json-results-view.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class JsonResultsViewComponent {

  instanceJson: string[];
  allInstancesJson: string = '';
  private _instanceSubscription: Subscription;

  constructor(private changeDetector: ChangeDetectorRef) {
  }

  private _instances$: Observable<InstanceLike>;

  @Input()
  get instances$(): Observable<InstanceLike> {
    return this._instances$;
  }

  set instances$(value: Observable<InstanceLike>) {
    if (value === this._instances$) {
      return;
    }
    if (this._instanceSubscription) {
      this._instanceSubscription.unsubscribe();
    }
    this._instances$ = value;
    this.instanceJson = [];
    this.allInstancesJson = '';
    this._instanceSubscription = this._instances$.subscribe(next => {
      let json = JSON.stringify(next.value, null, 3);
      this.instanceJson.push(json)
      const concatenatedJson = (this.allInstancesJson.length > 0) ?
        this.allInstancesJson + '\n\n' + json :
        json;
      this.allInstancesJson = concatenatedJson;
      this.changeDetector.markForCheck();
    });
  }
}
