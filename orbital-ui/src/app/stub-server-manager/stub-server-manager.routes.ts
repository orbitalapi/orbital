import {Route} from "@angular/router";
import {StubServerManagerComponent} from "./stub-server-manager.component";
import {StubServerDisplayComponent} from "./stub-server-display.component";

export const stubServersRoutes:Route[] = [
  {
    path: '',
    component: StubServerManagerComponent,
    children: [
      {
        path: 'stacks/:packageId/:stackId/:componentId',
        component: StubServerDisplayComponent
      }
    ]
  }
]
