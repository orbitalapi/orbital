import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TuiAvatarModule, TuiIslandModule, TuiStepperModule } from '@taiga-ui/kit';
import { TuiButtonModule } from '@taiga-ui/core';
import { UiCustomisations } from '../../../environments/ui-customisations';
import { ProjectSourceConfigModule } from '../../project-import/project-source-config/project-source-config.module';
import { ProjectListComponent } from './project-list/project-list.component';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-add-project',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    TuiStepperModule,
    TuiIslandModule,
    TuiButtonModule,
    TuiAvatarModule,
    ProjectSourceConfigModule,
    ProjectListComponent
  ],
  templateUrl: './add-project.component.html',
  styleUrls: ['./add-project.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AddProjectComponent {
  readonly uiConfig = UiCustomisations;

  step: 'options' | 'projectAdded' | 'gitRepo' | 'localDisk' | 'microService' = 'options';
  hasAddedProject: boolean;
  projectCount: number;
  projectCountPluralMap = {
    '=0': 'no projects',
    '=1': '1 project',
    'other': '# projects'
  }
}
