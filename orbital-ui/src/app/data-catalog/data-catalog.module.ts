import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DataCatalogSearchComponent } from './search/data-catalog-search.component';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { DataCatalogSearchResultCardComponent } from './search/data-catalog-search-result-card.component';
import {MarkdownModule, MARKED_OPTIONS} from 'ngx-markdown';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { OperationBadgeModule } from '../operation-badge/operation-badge.module';
import { DataCatalogContainerComponent } from './search/data-catalog-container.component';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { HeaderBarModule } from '../header-bar/header-bar.module';
import { TypeListComponent } from 'src/app/type-list/type-list.component';
import { AuthGuard } from 'src/app/services/auth.guard';
import { VynePrivileges } from 'src/app/services/user-info.service';
import { TypeViewerContainerComponent } from 'src/app/type-viewer/type-viewer-container.component';
import { TypeViewerModule } from 'src/app/type-viewer/type-viewer.module';
import { TypeListModule } from 'src/app/type-list/type-list.module';
import { HeaderComponentLayoutModule } from 'src/app/header-component-layout/header-component-layout.module';
import { TuiProgressModule, TuiStepperModule, TuiTabsModule } from '@taiga-ui/kit';
import { SchemaDiagramModule } from 'src/app/schema-diagram/schema-diagram.module';
import { UiCustomisations } from '../../environments/ui-customisations';
import markedAlert from 'marked-alert'
import {CustomMarkdownRenderer} from "../markdown-utils/markdown-custom-renderer";

@NgModule({
  declarations: [DataCatalogSearchComponent, DataCatalogSearchResultCardComponent, DataCatalogContainerComponent],
  exports: [DataCatalogSearchComponent, DataCatalogContainerComponent],
  imports: [
    CommonModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    MarkdownModule.forRoot({
      markedOptions: {
        provide: MARKED_OPTIONS,
        useFactory: (renderer: CustomMarkdownRenderer) => {
          return {
            renderer
          }
        },
        deps: [CustomMarkdownRenderer]
      },
      markedExtensions: [markedAlert()],
    }),
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    OperationBadgeModule,
    MatIconModule,
    MatProgressBarModule,
    HeaderBarModule,
    TypeViewerModule,
    TypeListModule,
    HeaderComponentLayoutModule,
    TuiTabsModule,
    SchemaDiagramModule,
    TuiStepperModule,
    TuiProgressModule,
    RouterModule.forChild([
      { path: '', component: DataCatalogContainerComponent, title: `${UiCustomisations.productName}: Catalog` },
      { path: 'diagram', component: DataCatalogContainerComponent, title: `${UiCustomisations.productName}: Catalog` },
      {
        path: 'browse',
        component: TypeListComponent,
        canActivate: [AuthGuard],
        data: { requiredAuthority: VynePrivileges.BrowseSchema },
        title: `${UiCustomisations.productName}: Catalog`
      },
      {
        path: ':typeName',
        component: TypeViewerContainerComponent,
        canActivate: [AuthGuard],
        data: { requiredAuthority: VynePrivileges.BrowseSchema },
        title: `${UiCustomisations.productName}: Catalog`
      }
    ])
  ]
})
export class DataCatalogModule {
}
