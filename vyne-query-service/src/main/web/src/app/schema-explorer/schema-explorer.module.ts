import { NgModule } from '@angular/core';
import { SchemaExplorerComponent } from './schema-explorer.component';
import { SearchModule } from '../search/search.module';
import { MatToolbarModule } from '@angular/material/toolbar';
import { CommonModule } from '@angular/common';
import { CodeViewerModule } from '../code-viewer/code-viewer.module';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatStepperModule } from '@angular/material/stepper';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { ReactiveFormsModule } from '@angular/forms';
import { CovalentHighlightModule } from '@covalent/highlight';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { HeaderBarModule } from '../header-bar/header-bar.module';
import { RouterModule } from '@angular/router';
import { SchemaExplorerContainerComponent } from './schema-explorer-container.component';
import { PackageViewerModule } from '../package-viewer/package-viewer.module';
import { ChangelogModule } from '../changelog/changelog.module';
import { SchemaSummaryViewComponent } from './schema-summary-view.component';
import { SimpleBadgeListModule } from '../simple-badge-list/simple-badge-list.module';
import { TuiButtonModule, TuiNotificationModule } from '@taiga-ui/core';
import { TuiTabsModule } from '@taiga-ui/kit';
import { SchemaExplorerTableModule } from 'src/app/schema-explorer-table/schema-explorer-table.module';
import {
  OrbitalSchemaExplorerContainerComponent
} from 'src/app/schema-explorer/orbital-schema-explorer-container.component';
import { ChangesetSelectorModule } from '../changeset-selector/changeset-selector.module';
import { SchemaSettingsComponent } from './schema-settings.component';
import { SchemaSourceConfigModule } from 'src/app/schema-source-config/schema-source-config.module';


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
    SchemaExplorerTableModule,
    SimpleBadgeListModule,
    TuiButtonModule,
    SchemaSourceConfigModule,
    RouterModule.forChild([
      {
        path: '',
        component: SchemaExplorerContainerComponent,
        children: [
          {
            path: '', component: SchemaSummaryViewComponent
          },
          {
            path: ':packageName', component: SchemaExplorerComponent
          }
        ]
      },
    ]),
    TuiTabsModule,
    ChangesetSelectorModule,
    TuiNotificationModule,
  ],
  exports: [SchemaExplorerComponent],
  declarations: [SchemaExplorerComponent, SchemaExplorerContainerComponent, SchemaSummaryViewComponent, OrbitalSchemaExplorerContainerComponent, SchemaSettingsComponent],
  providers: [],
})
export class SchemaExplorerModule {
}

