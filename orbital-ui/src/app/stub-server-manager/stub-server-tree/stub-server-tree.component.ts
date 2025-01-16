import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import { CommonModule} from '@angular/common';
import {RouterLink, RouterLinkActive, RouterOutlet} from "@angular/router";
import {AngularSplitModule} from "angular-split";
import {TuiBadge, TuiTree} from '@taiga-ui/kit';
import {ComponentInfoWithState, NebulaStacksResponse} from 'src/app/services/stubs-api.service';
import {ComponentStateIconComponent} from "./component-state-icon.component";

@Component({
  selector: 'app-stub-server-tree',
  standalone: true,
  imports: [
    TuiTree,
    CommonModule,
    RouterLinkActive,
    RouterLink,
    AngularSplitModule,
    RouterOutlet,
    ComponentStateIconComponent,
    TuiBadge,
  ],
  template: `
    <as-split direction="horizontal" unit="pixel">
      <as-split-area size="360">
        <ng-container [tuiTreeController]="true" *ngIf="stackState">
          @for (stackKeyValue of stackState?.stacks | keyvalue; track stackKeyValue.key) {
            <tui-tree-item class="root-tree-item">
              <div class="tree-package-icon">
              <span>
                <img src="assets/img/tabler/package.svg">
                {{ getPackageName(stackKeyValue.key) }}
              </span>
                <span>
                <img src="assets/img/tabler/file-description.svg">
                  {{ getStackFilename(stackKeyValue.key) }}
              </span>
              </div>

              @for (stackComponent of stackKeyValue.value; let i=$index; track i) {
                <tui-tree-item class="show-tree-decoration">
                  <span
                    class="is-navigable tree-item-label"
                    [routerLink]="getStackPath(stackKeyValue.key, stackComponent.name)"
                    [routerLinkActiveOptions]="{exact: true}"
                    routerLinkActive="active"
                  >
                  <app-component-state-icon [state]="stackComponent.state"></app-component-state-icon>
                    <!--<tui-badge appearance="badgeState" size="s">{{stackComponent.state.state}}</tui-badge>-->
                  <img class="tree-icon" [src]=serviceIcon(stackComponent.type)>{{ getComponentDisplayName(stackComponent) }}
                  </span>
                </tui-tree-item>
              }

            </tui-tree-item>
          }

        </ng-container>
      </as-split-area>
      <as-split-area>
        <router-outlet></router-outlet>
        <div class="no-route-selected">Click on a service on the left to view it's details here</div>
      </as-split-area>
    </as-split>
  `,
  styleUrl: './stub-server-tree.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StubServerTreeComponent {

  @Input()
  stackState: NebulaStacksResponse

  getStackPath(stackName: string, componentType: string): string {
    const stackUri = stackName.replace('[','')
      .replace(']','')
    const [packageId, stackId] = stackUri.split('/', 2)
    // path: 'stacks/:packageId/:stackId/:componentId',
    return `stacks/${packageId}/${stackId}/${componentType}`;
  }

  getPackageName(stackName: string): string {
    const packageName = stackName.split(']')[0].replace('[','')
    return packageName
  }
  getStackFilename(stackName: string): string {
    return stackName.substring(stackName.indexOf(']') + 2)
  }
  getComponentDisplayName(componentInfo: ComponentInfoWithState): string {
    if (componentInfo.name === componentInfo.type) {
      return componentInfo.name
    } else {
      return `${componentInfo.name} (${componentInfo.type})`
    }
  }

  serviceIcon(serviceKind: string) {
    switch (serviceKind) {
      case 'postgres' :
      case 'mysql':
      case 'mssql':
        return 'assets/img/tabler/database.svg'
      case 'kafka' :
        return 'assets/img/data-source-icons/kafka-icon.svg'
      case 'http' :
        return 'assets/img/tabler/api.svg';
      case 's3':
        return 'assets/img/tabler/bucket.svg'
      case 'hazelcast':
        return 'assets/img/data-source-icons/hazelcast_node.svg'
      default :
        return 'assets/img/chart-icons/api-icon.svg'
    }
  }
}

