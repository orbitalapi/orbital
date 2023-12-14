import {Component} from '@angular/core';
import {WorkspaceMembershipDto, WorkspacesService} from "../services/workspaces.service";
import {Observable} from "rxjs/internal/Observable";
import {Router} from "@angular/router";

@Component({
  selector: 'app-workspace-selector',
  template: `
    <tui-select
      [stringify]="stringifyWorkspace"
      [ngModel]="activeMembership$ | async"
      tuiTextfieldSize="s"
      (ngModelChange)="selectedWorkspaceChanged($event)">
      Workspace
      <tui-data-list *tuiDataList>
        <button
          tuiOption
          class="link small"
          (click)="createNewWorkspace()"
        >
          <tui-svg src="tuiIconPlus" class="icon"></tui-svg>
          <span class="small">New...</span>
        </button>
        <button *ngFor="let workspaceMembership of workspaces | async" tuiOption
                [value]="workspaceMembership">{{ workspaceMembership.workspace.name }}</button>
      </tui-data-list>
    </tui-select>
  `,
  styleUrls: ['./workspace-selector.component.scss']
})
export class WorkspaceSelectorComponent {
  workspaces: Observable<WorkspaceMembershipDto[]>;


  readonly stringifyWorkspace = (item: WorkspaceMembershipDto) => item.workspace?.name;
  readonly activeMembership$: Observable<WorkspaceMembershipDto>;

  constructor(private workspaceService: WorkspacesService, private router: Router) {
    this.workspaces = workspaceService.getCurrentWorkspaceMemberships()
    this.activeMembership$ = workspaceService.activeWorkspaceMembership$;
  }

  createNewWorkspace() {
    this.router.navigate(['/workspace', 'new'])
  }

  selectedWorkspaceChanged($event: WorkspaceMembershipDto) {
    this.workspaceService.switchWorkspace($event.workspace.id)
  }
}
