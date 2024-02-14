import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TuiButtonModule } from '@taiga-ui/core';
import { TuiAvatarModule, TuiIslandModule } from '@taiga-ui/kit';
import { UiCustomisations } from '../../../environments/ui-customisations';

@Component({
  selector: 'app-explore',
  standalone: true,
  imports: [CommonModule, TuiAvatarModule, TuiButtonModule, TuiIslandModule, RouterLink],
  templateUrl: './explore.component.html',
  styleUrls: ['./explore.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExploreComponent {
  readonly uiConfig = UiCustomisations;
}
