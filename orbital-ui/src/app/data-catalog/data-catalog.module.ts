import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DataCatalogSearchComponent } from './search/data-catalog-search.component';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
import { MatLegacySelectModule as MatSelectModule } from '@angular/material/legacy-select';
import { MatLegacyTableModule as MatTableModule } from '@angular/material/legacy-table';
import { DataCatalogSearchResultCardComponent } from './search/data-catalog-search-result-card.component';
import { MarkdownModule } from 'ngx-markdown';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { OperationBadgeModule } from '../operation-badge/operation-badge.module';
import { DataCatalogContainerComponent } from './search/data-catalog-container.component';
import { MatLegacyProgressBarModule as MatProgressBarModule } from '@angular/material/legacy-progress-bar';
import { MatLegacyTooltipModule as MatTooltipModule } from '@angular/material/legacy-tooltip';
import { HeaderBarModule } from '../header-bar/header-bar.module';
import { TypeListComponent } from 'src/app/type-list/type-list.component';
import { AuthGuard } from 'src/app/services/auth.guard';
import { VynePrivileges } from 'src/app/services/user-info.service';
import { TypeViewerContainerComponent } from 'src/app/type-viewer/type-viewer-container.component';
import { TypeViewerModule } from 'src/app/type-viewer/type-viewer.module';
import { TypeListModule } from 'src/app/type-list/type-list.module';
import { HeaderComponentLayoutModule } from 'src/app/header-component-layout/header-component-layout.module';
import { TuiTabsModule } from '@taiga-ui/kit';
import { SchemaDiagramModule } from 'src/app/schema-diagram/schema-diagram.module';


@NgModule({
  declarations: [DataCatalogSearchComponent, DataCatalogSearchResultCardComponent, DataCatalogContainerComponent],
  exports: [DataCatalogSearchComponent, DataCatalogContainerComponent],
  imports: [
    CommonModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    MarkdownModule.forRoot(),
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    OperationBadgeModule,
    MatIconModule,
    MatProgressBarModule,
    MatTooltipModule,
    HeaderBarModule,
    TypeViewerModule,
    TypeListModule,
    RouterModule.forChild([
      { path: '', component: DataCatalogContainerComponent },
      {
        path: 'browse',
        component: TypeListComponent,
        canActivate: [AuthGuard],
        data: { requiredAuthority: VynePrivileges.BrowseCatalog }
      },
      {
        path: ':typeName',
        component: TypeViewerContainerComponent,
        canActivate: [AuthGuard],
        data: { requiredAuthority: VynePrivileges.BrowseCatalog }
      }
    ]),
    HeaderComponentLayoutModule,
    TuiTabsModule,
    SchemaDiagramModule
  ]
})
export class DataCatalogModule {
}
