import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {DataCatalogSearchComponent} from './search/data-catalog-search.component';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatTableModule} from '@angular/material/table';
import {DataCatalogSearchResultCardComponent} from './search/data-catalog-search-result-card.component';
import {MarkdownModule} from 'ngx-markdown';
import {RouterModule} from '@angular/router';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatIconModule} from '@angular/material/icon';
import {OperationBadgeModule} from '../operation-badge/operation-badge.module';
import { DataCatalogContainerComponent } from './search/data-catalog-container.component';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatTooltipModule} from '@angular/material/tooltip';
import {HeaderBarModule} from '../header-bar/header-bar.module';


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
        HeaderBarModule
    ]
})
export class DataCatalogModule {
}
