import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TuiAvatarModule, TuiIslandModule, TuiStepperModule } from '@taiga-ui/kit';
import { TuiButtonModule } from '@taiga-ui/core';
import { UiCustomisations } from '../../../environments/ui-customisations';
import { SchemaSourceConfigModule } from '../../schema-source-config/schema-source-config.module';
import { ProjectListComponent } from './project-list/project-list.component';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-create-project',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    TuiStepperModule,
    TuiIslandModule,
    TuiButtonModule,
    TuiAvatarModule,
    SchemaSourceConfigModule,
    ProjectListComponent
  ],
  templateUrl: './create-project.component.html',
  styleUrls: ['./create-project.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CreateProjectComponent {
  readonly uiConfig = UiCustomisations;

  step: 'options' | 'projectCreated' | 'gitRepo' | 'localDisk' | 'microService' = 'options';
  hasCreatedProject: boolean;
}
