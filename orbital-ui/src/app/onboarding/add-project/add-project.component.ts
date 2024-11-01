import { TuiButton } from "@taiga-ui/core";
import { TuiIslandDirective } from "@taiga-ui/legacy";
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TuiStepper, TuiAvatar } from '@taiga-ui/kit';
import { UiCustomisations } from '../../../environments/ui-customisations';
import {FileConfigComponent} from '../../project-import/project-source-config/file-config.component';
import {GitConfigComponent} from '../../project-import/project-source-config/git-config.component';
import { ProjectListComponent } from './project-list/project-list.component';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-add-project',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    TuiStepper,
    TuiIslandDirective,
    TuiButton,
    TuiAvatar,
    ProjectListComponent,
    FileConfigComponent,
    GitConfigComponent
  ],
  templateUrl: './add-project.component.html',
  styleUrls: ['./add-project.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AddProjectComponent {

  @Input()
  isOnboardingMode:boolean = true;

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
