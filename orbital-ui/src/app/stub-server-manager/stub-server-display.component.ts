import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject} from '@angular/core';
import {ComponentInfo, NebulaStacksResponse, StubsApiService} from "../services/stubs-api.service";
import {TypesService} from "../services/types.service";
import {ActivatedRoute, ParamMap} from "@angular/router";
import {combineLatestWith} from "rxjs";
import {KeyValuePipe, NgForOf, NgIf} from "@angular/common";
import {TuiAlertService, TuiButtonModule, TuiNotification} from "@taiga-ui/core";
import {Clipboard} from "@angular/cdk/clipboard";

@Component({
  selector: 'app-stub-server-display',
  standalone: true,
  imports: [
    NgIf,
    KeyValuePipe,
    NgForOf,
    TuiButtonModule
  ],
  template: `
      <h2>{{ componentId }}</h2>

      <section *ngIf="componentInfo?.container">
        <h3>Container Info</h3>
        <table class="table">
          <tr>
            <td class="label-col">Image name</td>
            <td>{{ componentInfo.container.imageName }}</td>
          </tr>
          <tr>
            <td class="label-col">Container id</td>
            <td>{{ componentInfo.container.containerId }}</td>
          </tr>
          <tr>
            <td class="label-col">Container name</td>
            <td>{{ componentInfo.container.containerName }}</td>
          </tr>
        </table>
      </section>

      <section *ngIf="componentInfo?.componentConfig">
        <h3>Service info</h3>
        <table class="table">
          <tr *ngFor="let serviceInfoParam of componentInfo.componentConfig | keyvalue">
            <td class="label-col">{{ serviceInfoParam.key }}</td>
            <td>{{ serviceInfoParam.value }}<button class="copyButton" tuiButton appearance="flat" size="xs" (click)="copyToClipboard(serviceInfoParam.value)" ><img src="assets/img/tabler/clipboard.svg"></button></td>
          </tr>
        </table>
      </section>

      <section *ngIf="environmentVariables">
        <h3>Environment variables</h3>

        <span class="subheader">This component has published environment variables for your connection definitions.</span>
        <span class="subheader">Each variable is provided under multiple names, ranging from the most convenient to the most specific, to help prevent naming conflicts. Choose the one that best fits your needs.</span>
        <table class="table">
          <tr *ngFor="let envVariable of environmentVariables | keyvalue">
            <td class="label-col">{{ envVariable.key }}<button class="copyButton" tuiButton appearance="flat" size="xs" (click)="copyToClipboard(envVariable.key)" ><img src="assets/img/tabler/clipboard.svg"></button></td>
            <td>{{ envVariable.value }}<button class="copyButton" tuiButton appearance="flat" size="xs" (click)="copyToClipboard(envVariable.value)" ><img src="assets/img/tabler/clipboard.svg"></button></td>
          </tr>
        </table>
      </section>
  `,
  styleUrl: './stub-server-display.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StubServerDisplayComponent {

  componentInfo: ComponentInfo;
  environmentVariables: { [key: string]: string };

  packageId: string;
  stackId: string;
  componentId: string;

  constructor(stubsService: StubsApiService,
              route: ActivatedRoute,
              private clipboard: Clipboard,
              @Inject(TuiAlertService) private readonly alerts: TuiAlertService,
              private changeDetector: ChangeDetectorRef
  ) {
    stubsService.getStubStates()
      .pipe(combineLatestWith(route.paramMap)).subscribe((next: [NebulaStacksResponse, ParamMap]) => {
      this.packageId = next[1].get('packageId');
      this.stackId = next[1].get('stackId');
      this.componentId = next[1].get('componentId');
      const stacksResponse: NebulaStacksResponse = next[0]

      const stackKey = `[${this.packageId}]/${this.stackId}`;
      this.componentInfo = stacksResponse.stacks[stackKey][this.componentId];
      this.environmentVariables = stacksResponse.environmentVariables[stackKey][this.componentId]

      this.changeDetector.markForCheck();
    })
  }

  copyToClipboard(value:string) {
    this.clipboard.copy(value)
    this.alerts.open('Copied to clipboard', {status: TuiNotification.Success})
      .subscribe()
  }
}
