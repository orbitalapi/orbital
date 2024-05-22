import {Component, EventEmitter, HostBinding, Input, Output} from '@angular/core';
import { CommonModule } from '@angular/common';
import {MatIconModule} from "@angular/material/icon";
import {TuiHintModule, TuiLinkModule} from "@taiga-ui/core";
import {openNewSiteAndCancel} from "../toolbar/playground-toolbar.component";
import {environment} from "../../../voyager-app/environments/environment";

@Component({
  selector: 'app-voyager-sidebar',
  standalone: true,
  imports: [CommonModule, MatIconModule, TuiLinkModule, TuiHintModule],
  template: `
    <button class="icon-toggle-button" (click)="toggleQueryPanel()" [class.active]="showQueryPanel" [tuiHint]="'Toggle query panel'">
      <mat-icon svgIcon="file-search"></mat-icon>
    </button>
    <button class="icon-toggle-button" (click)="toggleDiagram()" [class.active]="showDiagram" [tuiHint]="'Toggle diagram'">
      <mat-icon svgIcon="route-square-2"></mat-icon>
    </button>

    <div class="spacer"></div>
    <button class="icon-toggle-button" *ngIf="showCopyCodeButton" (click)="copyDevCode.emit()" [tuiHint]="'Copy dev code'">
      <mat-icon svgIcon="code-circle"></mat-icon>
    </button>

    <a tuiLink href="https://github.com/orbitalapi/orbital" target="_blank"
       (click)="openNewSite($event, 'https://github.com/orbitalapi/orbital')">
      <mat-icon svgIcon="brandGitHub"></mat-icon>
    </a>
    <a tuiLink [href]="slackInviteLink" target="_blank"
       (click)="openNewSite($event, slackInviteLink)"
    >
      <mat-icon svgIcon="slack"></mat-icon>
    </a>

  `,
  styleUrls: ['./voyager-sidebar.component.scss']
})
export class VoyagerSidebarComponent {
  @Input()
  side: 'left' | 'right' = 'left';

  // dev-tool button for copying a snippet at something we can then
  // embed into Voyager.
  showCopyCodeButton = !environment.production

  @Input()
  showDiagram: boolean;

  @Output()
  showDiagramChange = new EventEmitter<boolean>()

  @Input()
  showQueryPanel: boolean;

  @Output()
  showQueryPanelChange = new EventEmitter<boolean>()

  @Output()
  copyDevCode = new EventEmitter<void>()

  @HostBinding('class') get sideClass() {
    return this.side;
  }

  readonly slackInviteLink = 'https://join.slack.com/t/orbitalapi/shared_invite/zt-697laanr-DHGXXak5slqsY9DqwrkzHg';

  openNewSite(event: Event, url: string) {
    openNewSiteAndCancel(event, url)
  }


  toggleDiagram() {
    this.showDiagram = !this.showDiagram;
    this.showDiagramChange.emit(this.showDiagram);
  }
  toggleQueryPanel() {
    this.showQueryPanel = !this.showQueryPanel;
    this.showQueryPanelChange.emit(this.showQueryPanel);
  }
}
