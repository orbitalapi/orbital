import {ChangeDetectionStrategy, ChangeDetectorRef, Component} from '@angular/core';
import {AsyncPipe, NgIf} from "@angular/common";
import {UiCustomisations} from "../../environments/ui-customisations";
import {HeaderComponentLayoutModule} from "../header-component-layout/header-component-layout.module";
import {StubServerTreeComponent} from "./stub-server-tree/stub-server-tree.component";
import {TypesService} from "../services/types.service";
import {Observable} from "rxjs";
import {NebulaStacksResponse, StubsApiService} from '../services/stubs-api.service';
import {isNullOrUndefined} from "../utils/utils";
import {ConnectionStatusComponent} from "../data-source-manager/connection-status/connection-status.component";
import { ConnectionStatus } from '../db-connection-editor/db-importer.service';

@Component({
  selector: 'app-stub-server-manager',
  standalone: true,
  imports: [
    AsyncPipe,
    HeaderComponentLayoutModule,
    StubServerTreeComponent,
    NgIf,
    ConnectionStatusComponent
  ],
  template: `
    <app-header-component-layout
      title="Stub servers"
      [description]="'Stub servers are temporary APIs, databases, Kafka instances etc., used for testing or development'"
    >
      <ng-container ngProjectAs="header-components">
        <div class="server-status" *ngIf="serverStatus">
          <app-connection-status [status]="serverStatus"></app-connection-status>
          <span *ngIf="serverStatus.status != 'OK'">{{ serverStatus.message }}</span>
        </div>
      </ng-container>
      <app-stub-server-tree *ngIf="showServerTree" [stackState]="stacksState"></app-stub-server-tree>
      <div *ngIf="hasErrors" class="errors-panel">
        <h3>Stub servers could not be loaded:</h3>
        <span>{{ stacksState.error }}</span>
        <span>To troubleshoot, check out the <a class="link" [href]="UiCustomisations.docsLinks.nebulaDocs"
                                                target="_blank">docs</a></span>
      </div>
      <div *ngIf="isEmpty" class="empty-state-container">
        <img src="assets/img/illustrations/data-center.svg">
        <p>No stub servers have been defined yet. Learn more about how to create stubs in the <a class="link"
                                                                                                 target="_blank"
                                                                                                 [href]="UiCustomisations.docsLinks.nebulaDocs">docs</a>.
        </p>
      </div>
    </app-header-component-layout>
  `,
  styleUrl: './stub-server-manager.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StubServerManagerComponent {

  protected readonly UiCustomisations = UiCustomisations;
  stacksState: NebulaStacksResponse;
  serverStatus: ConnectionStatus;

  constructor(stubsService: StubsApiService,
              changeDetectorRef: ChangeDetectorRef
  ) {
    stubsService.getStubStateStream()
      .subscribe(next => {
        this.stacksState = next;
        changeDetectorRef.markForCheck();
      })
    stubsService.getStubServerStatus()
      .subscribe(next => {
        this.serverStatus = next;
        changeDetectorRef.markForCheck();
      })
  }

  get isEmpty(): boolean {
    return !this.hasErrors && !this.showServerTree;
  }

  get showServerTree(): boolean {
    const stacks = this.stacksState?.stacks || {}
    const hasErrors = this.hasErrors
    return Object.keys(stacks).length > 0 && !hasErrors
  }

  get hasErrors(): boolean {
    if (!isNullOrUndefined(this.stacksState)) {
      return this.stacksState.hasError
    } else {
      return false
    }
  }
}
