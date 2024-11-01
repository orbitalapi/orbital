import { TuiTextfieldControllerModule, TuiInputModule } from "@taiga-ui/legacy";
import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {CatalogExplorerPanelComponent} from './catalog-explorer-panel.component';
import { TuiTree } from "@taiga-ui/kit";
import { TuiLoader, TuiIcon, TuiButton, TuiHint } from "@taiga-ui/core";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatProgressBarModule} from "@angular/material/progress-bar";
import {CatalogPanelSearchResults} from './catalog-panel-search-results.component';
import {CatalogTreeComponent} from './catalog-tree.component';
import {CatalogEntryLineComponent} from './catalog-entry-line.component';


@NgModule({
  declarations: [
    CatalogExplorerPanelComponent,
    CatalogPanelSearchResults,
    CatalogTreeComponent,
    CatalogEntryLineComponent,
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
      TuiLoader,
      ...TuiTree,
      ...TuiHint,
      TuiIcon,
      TuiButton
    ]
})
export class CatalogExplorerPanelModule { }
