import {ChangeDetectionStrategy, Component, computed, signal, WritableSignal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {UiCustomisations} from "../../environments/ui-customisations";
import {HeaderComponentLayoutComponent} from "../header-component-layout/header-component-layout.component";
import {StubServerTreeComponent} from "./stub-server-tree/stub-server-tree.component";
import {NebulaStacksResponse, StubsApiService} from '../services/stubs-api.service';
import {ConnectionStatusComponent} from "../data-source-manager/connection-status/connection-status.component";
import {ConnectionStatus} from '../db-connection-editor/db-importer.service';

@Component({
  selector: 'app-stub-server-manager',
  standalone: true,
  imports: [
    CommonModule,
    HeaderComponentLayoutComponent,
    StubServerTreeComponent,
    ConnectionStatusComponent
  ],
  template: `
    <app-header-component-layout
      title="Stub servers"
      [description]="'Stub servers are temporary APIs, databases, Kafka instances etc., used for testing or development'"
    >
      <ng-container ngProjectAs="header-components">
        <div class="server-status" *ngIf="serverStatus()">
          <app-connection-status [status]="serverStatus()"></app-connection-status>
          <span *ngIf="serverStatus().status != 'OK'">{{ serverStatus().message }}</span>
        </div>
      </ng-container>
      <app-stub-server-tree *ngIf="showServerTree()" [stackState]="stacksState()"></app-stub-server-tree>
      <div *ngIf="hasErrors()" class="errors-panel">
        <h3>Stub servers could not be loaded:</h3>
        <span>{{ stacksState().error }}</span>
        <span>To troubleshoot, check out the <a class="link" [href]="UiCustomisations.docsLinks.nebulaDocs"
                                                target="_blank">docs</a></span>
      </div>
      <div *ngIf="isEmpty()" class="empty-state-container">
        <img src="assets/img/illustrations/data-center.svg">
        <p>No stub servers have been defined yet. Learn more about how to create stubs in the
          <a class="link" target="_blank" [href]="UiCustomisations.docsLinks.nebulaDocs">docs</a>.
        </p>
      </div>
    </app-header-component-layout>
  `,
  styleUrl: './stub-server-manager.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StubServerManagerComponent {

  stacksState: WritableSignal<NebulaStacksResponse> = signal(null);
  serverStatus: WritableSignal<ConnectionStatus> = signal(null);
  protected readonly UiCustomisations = UiCustomisations;

  constructor(
    stubsService: StubsApiService,
  ) {
    stubsService.getStubStateStream()
      .subscribe(next => {
        this.stacksState.set(next);
      })
    stubsService.getStubServerStatus()
      .subscribe(next => {
        if (next.message !== this.serverStatus()?.message)
          this.serverStatus.set(next);
      })
  }

  isEmpty = computed<boolean>(() => {
    return !this.hasErrors() && !this.showServerTree();
  })

  showServerTree = computed<boolean>(() => {
    const stacks = this.stacksState()?.stacks || {}
    const hasErrors = this.hasErrors()
    return Object.keys(stacks).length > 0 && !hasErrors
  })

  hasErrors = computed<boolean>(() => {
    const state = this.stacksState();
    return state?.hasError ?? false;
  });
}
