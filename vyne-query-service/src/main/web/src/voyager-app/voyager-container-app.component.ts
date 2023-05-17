import {ChangeDetectionStrategy, Component} from '@angular/core';

/**
 * A simple wrapper, to allow injection of the router, so we can access share urls etc.
 */
@Component({
  selector: 'voyager-container-app',
  template: `
    <tui-root>
      <router-outlet></router-outlet>
    </tui-root>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class VoyagerContainerAppComponent {

}
