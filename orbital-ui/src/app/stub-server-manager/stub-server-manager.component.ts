import {Component} from '@angular/core';
import {AsyncPipe} from "@angular/common";
import {UiCustomisations} from "../../environments/ui-customisations";
import {HeaderComponentLayoutModule} from "../header-component-layout/header-component-layout.module";
import {StubServerTreeComponent} from "./stub-server-tree/stub-server-tree.component";
import {TypesService} from "../services/types.service";
import {Observable, switchMap} from "rxjs";
import {NebulaStacksResponse, StubsApiService} from '../services/stubs-api.service';

@Component({
  selector: 'app-stub-server-manager',
  standalone: true,
  imports: [
    AsyncPipe,
    HeaderComponentLayoutModule,
    StubServerTreeComponent
  ],
  template: `
    <app-header-component-layout
      title="Stub servers"
      [description]="'Stub servers are temporary APIs, databases, Kafka instances etc., used for testing or development'"
    >
      <app-stub-server-tree [stackState]="stacksState"></app-stub-server-tree>
    </app-header-component-layout>
  `,
  styleUrl: './stub-server-manager.component.scss'
})
export class StubServerManagerComponent {

  protected readonly UiCustomisations = UiCustomisations;
  stacksState: NebulaStacksResponse;
  constructor(stubsService: StubsApiService,
              typeService: TypesService
  ) {
    // Note: we don't need the schema here,
    // but we want to update our stubs list whenever
    // the schema changes
    typeService.getTypes()
      .pipe(
        switchMap(() => stubsService.getStubStates())
      ).subscribe(next => {
        this.stacksState = next;
      })
  }
}
