import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {TuiSegmented} from '@taiga-ui/kit';
import {
  ComponentInfo,
  ComponentInfoWithState,
  NebulaStacksResponse,
  StubsApiService
} from "../services/stubs-api.service";
import {ActivatedRoute, ParamMap} from "@angular/router";
import {combineLatestWith} from "rxjs";
import {CommonModule} from '@angular/common';
import {TuiAlertService, TuiButton, TuiHintOverflow} from '@taiga-ui/core';
import {Clipboard} from "@angular/cdk/clipboard";

@Component({
  selector: 'app-stub-server-display',
  standalone: true,
  imports: [
    CommonModule,
    TuiButton,
    TuiHintOverflow,
    TuiSegmented,
  ],
  template: `
    <h2>{{ componentId }}</h2>
    <tui-segmented size="s" [(activeItemIndex)]="segmentActiveIndex">
      <button [class.active]="segmentActiveIndex === 0">Environment variables</button>
      <button [class.active]="segmentActiveIndex === 1">Container/Service</button>
    </tui-segmented>
    <section *ngIf="componentInfo?.container && segmentActiveIndex === 1">
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

    <section *ngIf="componentInfo?.componentConfig && segmentActiveIndex === 1">
      <h3>Service info</h3>
      <table class="table">
        <tr *ngFor="let serviceInfoParam of componentInfo.componentConfig | keyvalue">
          <td class="label-col" tuiHintOverflow tuiHintAppearance="dark">{{ serviceInfoParam.key }}</td>
          <td tuiHintOverflow tuiHintAppearance="dark">
            {{ serviceInfoParam.value }}
            <button class="copyButton" tuiIconButton appearance="flat" size="xs"
                    (click)="copyToClipboard(serviceInfoParam.value)"><img src="assets/img/tabler/clipboard.svg">
            </button>
          </td>
        </tr>
      </table>
    </section>

    <section *ngIf="environmentVariables && segmentActiveIndex === 0">
      <h3>Environment variables</h3>

      <span class="subheader">This component has published environment variables for your connection definitions.</span>
      <span class="subheader">Each variable is provided under multiple names, ranging from the most convenient to the most specific, to help prevent naming conflicts. Choose the one that best fits your needs.</span>
      <table class="table">
        <tr *ngFor="let envVariable of environmentVariables | keyvalue">
          <td class="label-col" tuiHintOverflow tuiHintAppearance="dark">
            {{ envVariable.key }}
          </td>
          <td class="copy-td">
            <button class="copyButton" tuiIconButton appearance="flat" size="xs"
                    (click)="copyToClipboard(envVariable.key)" title="click to copy to clipboard"><img
              src="assets/img/tabler/clipboard.svg"></button>
          </td>
          <td tuiHintOverflow tuiHintAppearance="dark">
            {{ envVariable.value }}
          </td>
          <td class="copy-td">
            <button class="copyButton" tuiIconButton appearance="flat" size="xs"
                    (click)="copyToClipboard(envVariable.value)" title="click to copy to clipboard"><img
              src="assets/img/tabler/clipboard.svg"></button>
          </td>
        </tr>
      </table>
    </section>
  `,
  styleUrl: './stub-server-display.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StubServerDisplayComponent {

  componentWithState: ComponentInfoWithState
  componentInfo: ComponentInfo;
  environmentVariables: { [key: string]: string };

  packageId: string;
  stackId: string;
  componentId: string;
  segmentActiveIndex: number = 0

  constructor(stubsService: StubsApiService,
              route: ActivatedRoute,
              private clipboard: Clipboard,
              @Inject(TuiAlertService) private readonly alerts: TuiAlertService,
              private changeDetector: ChangeDetectorRef
  ) {
    stubsService.getStubStateStream()
      .pipe(
        takeUntilDestroyed(),
        combineLatestWith(route.paramMap)
      )
      .subscribe((next: [NebulaStacksResponse, ParamMap]) => {
        console.log("getStubStateStream", next)
        this.packageId = next[1].get('packageId');
        this.stackId = next[1].get('stackId');
        this.componentId = next[1].get('componentId');
        const stacksResponse: NebulaStacksResponse = next[0]

        const stackKey = `[${this.packageId}]/${this.stackId}`;
        const stackState = stacksResponse.stacks[stackKey]
        this.componentWithState = stackState.find(componentWithState => componentWithState.name == this.componentId)
        this.componentInfo = this.componentWithState.componentInfo
        this.environmentVariables = stacksResponse.environmentVariables[stackKey][this.componentId]

        this.changeDetector.markForCheck();
      })
  }

  copyToClipboard(value:string) {
    this.clipboard.copy(value)
    this.alerts.open('Copied to clipboard', {appearance: 'success'})
      .subscribe()
  }
}
