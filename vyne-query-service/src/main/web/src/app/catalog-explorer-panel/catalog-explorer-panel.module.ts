import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CatalogExplorerPanelComponent } from './catalog-explorer-panel.component';
import {TuiInputModule, TuiTreeModule} from "@taiga-ui/kit";
import {TuiHintModule, TuiLoaderModule, TuiSvgModule, TuiTextfieldControllerModule} from "@taiga-ui/core";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatProgressBarModule} from "@angular/material/progress-bar";
import { CatalogPanelSearchResults } from './catalog-panel-search-results.component';
import { CatalogTreeComponent } from './catalog-tree.component';
import { CatalogEntryLineComponent } from './catalog-entry-line.component';
import { CatalogEntryDetailComponent } from './catalog-entry-detail.component';



@NgModule({
  declarations: [
    CatalogExplorerPanelComponent,
    CatalogPanelSearchResults,
    CatalogTreeComponent,
    CatalogEntryLineComponent,
    CatalogEntryDetailComponent
  ],
  exports: [
    CatalogExplorerPanelComponent,
    CatalogPanelSearchResults
  ],
    imports: [
        CommonModule,
        TuiInputModule,
        TuiTextfieldControllerModule,
        FormsModule,
        ReactiveFormsModule,
        MatProgressBarModule,
        TuiLoaderModule,
        TuiTreeModule,
        TuiHintModule,
        TuiSvgModule
    ]
})
export class CatalogExplorerPanelModule { }
