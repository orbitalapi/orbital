import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {TuiTreeModule} from "@taiga-ui/kit";
import {AsyncPipe, KeyValuePipe, NgForOf, NgIf} from "@angular/common";
import {NebulaStacksResponse} from 'src/app/services/stubs-api.service';
import {RouterLink, RouterLinkActive, RouterOutlet} from "@angular/router";
import {AngularSplitModule} from "angular-split";

@Component({
  selector: 'app-stub-server-tree',
  standalone: true,
  imports: [
    TuiTreeModule,
    KeyValuePipe,
    AsyncPipe,
    NgForOf,
    NgIf,
    RouterLinkActive,
    RouterLink,
    AngularSplitModule,
    RouterOutlet
  ],
  template: `
    <as-split direction="horizontal" unit="pixel">
      <as-split-area size="360">
        <ng-container [tuiTreeController]="true" *ngIf="stackState">
          <tui-tree-item class="root-tree-item" *ngFor="let stack of stackState?.stacks | keyvalue">
            <div class="tree-package-icon">
              <span>
                <img src="assets/img/tabler/package.svg">
          {{ getPackageName(stack.key) }}
              </span>
              <span>
                <img src="assets/img/tabler/file-description.svg">
                        {{ getStackFilename(stack.key) }}
              </span>
            </div>

            <tui-tree-item class="show-tree-decoration" *ngFor="let stackComponent of stack.value | keyvalue">
           <span
             class="is-navigable"
             [routerLink]="getStackPath(stack.key, stackComponent.key)"
             [routerLinkActiveOptions]="{exact: true}"
             routerLinkActive="active"
           >
              <img class="tree-icon" [src]=serviceIcon(stackComponent.key)>{{ stackComponent.key }}
            </span>
            </tui-tree-item>
          </tui-tree-item>
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
    return `stacks/${stackUri}/${componentType}`;
  }

  getPackageName(stackName: string): string {
    const packageName = stackName.split(']')[0].replace('[','')
    return packageName
  }
  getStackFilename(stackName: string): string {
    return stackName.substring(stackName.indexOf(']') + 2)
  }

  serviceIcon(serviceKind: string) {
    switch (serviceKind) {
      case 'postgres' :
      case 'mysql':
      case 'mssql':
        return 'assets/img/chart-icons/database-icon.svg'
      case 'kafka' :
        return 'assets/img/data-source-icons/kafka-icon.svg'
      case 'http' :
        return 'assets/img/chart-icons/api-icon.svg'
      default :
        return 'assets/img/chart-icons/api-icon.svg'
    }
  }
}
