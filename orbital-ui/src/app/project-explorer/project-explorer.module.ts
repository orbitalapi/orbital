import {NgModule} from '@angular/core';
import { ProjectImportComponent } from '../project-import/project-import.component';
import { AuthGuard } from '../services/auth.guard';
import { VynePrivileges } from '../services/user-info.service';
import {ProjectExplorerComponent} from './project-explorer.component';
import {SearchModule} from '../search/search.module';
import {MatToolbarModule} from '@angular/material/toolbar';
import {CommonModule} from '@angular/common';
import {CodeViewerModule} from '../code-viewer/code-viewer.module';
import {MatLegacyMenuModule as MatMenuModule} from '@angular/material/legacy-menu';
import {MatLegacyButtonModule as MatButtonModule} from '@angular/material/legacy-button';
import {MatLegacyProgressBarModule as MatProgressBarModule} from '@angular/material/legacy-progress-bar';
import {MatStepperModule} from '@angular/material/stepper';
import {MatLegacyFormFieldModule as MatFormFieldModule} from '@angular/material/legacy-form-field';
import {MatLegacySelectModule as MatSelectModule} from '@angular/material/legacy-select';
import {ReactiveFormsModule} from '@angular/forms';
import {CovalentHighlightModule} from '@covalent/highlight';
import {MatLegacyListModule as MatListModule} from '@angular/material/legacy-list';
import {MatIconModule} from '@angular/material/icon';
import {MatLegacyInputModule as MatInputModule} from '@angular/material/legacy-input';
import {HeaderBarModule} from '../header-bar/header-bar.module';
import {RouterModule} from '@angular/router';
import {ProjectExplorerContainerComponent} from './project-explorer-container.component';
import {PackageViewerModule} from '../package-viewer/package-viewer.module';
import {ChangelogModule} from '../changelog/changelog.module';
import {ProjectSummaryViewComponent} from './project-summary-view.component';
import {SimpleBadgeListModule} from '../simple-badge-list/simple-badge-list.module';
import {TuiButtonModule, TuiNotificationModule} from '@taiga-ui/core';
import {TuiTabsModule} from '@taiga-ui/kit';
import {SchemaMemberTypeExplorerModule} from 'src/app/schema-member-type-explorer/schema-member-type-explorer.module';
import {ChangesetSelectorModule} from '../changeset-selector/changeset-selector.module';
import {ProjectSettingsComponent} from './project-settings.component';
import {ProjectSourceConfigModule} from 'src/app/project-import/project-source-config/project-source-config.module';
import {ExpandingPanelSetModule} from "../expanding-panelset/expanding-panel-set.module";
import {UiCustomisations} from '../../environments/ui-customisations';
import {ProjectErrorListComponent} from './project-error-list.component';
import {HeaderComponentLayoutModule} from "../header-component-layout/header-component-layout.module";


@NgModule({
  imports: [
    CommonModule,
    MatMenuModule,
    MatButtonModule,
    SearchModule,
    MatToolbarModule,
    CodeViewerModule,
    MatProgressBarModule,
    MatStepperModule,
    MatFormFieldModule,
    MatSelectModule,
    ReactiveFormsModule,
    CovalentHighlightModule,
    MatListModule,
    MatIconModule,
    MatInputModule,
    HeaderBarModule,
    RouterModule,
    PackageViewerModule,
    ChangelogModule,
    SchemaMemberTypeExplorerModule,
    SimpleBadgeListModule,
    TuiButtonModule,
    ProjectSourceConfigModule,
    RouterModule.forChild([
      {
        path: 'project-import',
        component: ProjectImportComponent,
        canActivate: [AuthGuard],
        data: {requiredAuthority: VynePrivileges.EditSchema}
      },
      {
        path: '',
        component: ProjectExplorerContainerComponent,
        children: [
          {
            path: '',
            component: ProjectSummaryViewComponent,
            title: `${UiCustomisations.productName}: Projects`
          },
          {
            path: 'problems',
            component: ProjectErrorListComponent,
            title: `${UiCustomisations.productName}: Projects`
          },
          {
            path: ':packageName',
            component: ProjectExplorerComponent,
            title: `${UiCustomisations.productName}: Projects`
          },
          {
            path: ':packageName/:selectedTab',
            component: ProjectExplorerComponent,
            title: `${UiCustomisations.productName}: Projects`
          },
          {
            path: ':packageName/:selectedTab/**',
            component: ProjectExplorerComponent,
            title: `${UiCustomisations.productName}: Projects`
          }
        ]
      },
    ]),
    TuiTabsModule,
    ChangesetSelectorModule,
    TuiNotificationModule,
    ExpandingPanelSetModule,
    HeaderComponentLayoutModule,
  ],
  exports: [ProjectExplorerComponent],
  declarations: [ProjectExplorerComponent, ProjectExplorerContainerComponent, ProjectSummaryViewComponent, ProjectSettingsComponent, ProjectErrorListComponent],
  providers: [],
})
export class ProjectExplorerModule {
}

