import { NgModule } from '@angular/core';
import { SchemaExplorerComponent } from './schema-explorer.component';
import { SearchModule } from '../search/search.module';
import { MatToolbarModule } from '@angular/material/toolbar';
import { CommonModule } from '@angular/common';
import { CodeViewerModule } from '../code-viewer/code-viewer.module';
import { MatLegacyMenuModule as MatMenuModule } from '@angular/material/legacy-menu';
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { MatLegacyProgressBarModule as MatProgressBarModule } from '@angular/material/legacy-progress-bar';
import { MatStepperModule } from '@angular/material/stepper';
import { MatLegacyFormFieldModule as MatFormFieldModule } from '@angular/material/legacy-form-field';
import { MatLegacySelectModule as MatSelectModule } from '@angular/material/legacy-select';
import { ReactiveFormsModule } from '@angular/forms';
import { CovalentHighlightModule } from '@covalent/highlight';
import { MatLegacyListModule as MatListModule } from '@angular/material/legacy-list';
import { MatIconModule } from '@angular/material/icon';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
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
import { ChangesetSelectorModule } from '../changeset-selector/changeset-selector.module';
import { SchemaSettingsComponent } from './schema-settings.component';
import { SchemaSourceConfigModule } from 'src/app/schema-source-config/schema-source-config.module';
import {ExpandingPanelSetModule} from "../expanding-panelset/expanding-panel-set.module";


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
        ExpandingPanelSetModule,
    ],
  exports: [SchemaExplorerComponent],
  declarations: [SchemaExplorerComponent, SchemaExplorerContainerComponent, SchemaSummaryViewComponent, SchemaSettingsComponent],
  providers: [],
})
export class SchemaExplorerModule {
}

