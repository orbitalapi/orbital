import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TuiAvatarModule, TuiIslandModule, TuiStepperModule } from '@taiga-ui/kit';
import { TuiButtonModule } from '@taiga-ui/core';
import { UiCustomisations } from '../../../environments/ui-customisations';

@Component({
  selector: 'app-create-project',
  standalone: true,
  imports: [CommonModule, TuiStepperModule, TuiIslandModule, TuiButtonModule, TuiAvatarModule, RouterLink, RouterLinkActive],
  templateUrl: './create-project.component.html',
  styleUrls: ['./create-project.component.scss']
})
export class CreateProjectComponent {
  readonly uiConfig = UiCustomisations;
}
