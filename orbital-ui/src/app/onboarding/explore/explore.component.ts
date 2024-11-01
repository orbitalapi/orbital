import { TuiButton } from "@taiga-ui/core";
import { TuiAvatar } from "@taiga-ui/kit";
import { TuiIslandDirective } from "@taiga-ui/legacy";
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { UiCustomisations } from '../../../environments/ui-customisations';

@Component({
  selector: 'app-explore',
  standalone: true,
  imports: [CommonModule, TuiAvatar, TuiButton, TuiIslandDirective, RouterLink],
  templateUrl: './explore.component.html',
  styleUrls: ['./explore.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExploreComponent {
  readonly uiConfig = UiCustomisations;
}
