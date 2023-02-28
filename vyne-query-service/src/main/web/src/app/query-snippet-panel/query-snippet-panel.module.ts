import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { QuerySnippetPanelComponent } from './query-snippet-panel.component';
import { TuiAccordionModule, TuiDataListWrapperModule, TuiInputModule, TuiSelectModule } from '@taiga-ui/kit';
import { CovalentHighlightModule } from '@covalent/highlight';
import { QuerySnippetContainerComponent } from './query-snippet-container.component';
import { FormsModule } from '@angular/forms';
import { TuiButtonModule, TuiDataListModule, TuiRootModule, TuiTextfieldControllerModule } from '@taiga-ui/core';


@NgModule({
  declarations: [
    QuerySnippetPanelComponent,
    QuerySnippetContainerComponent
  ],
  exports: [
    QuerySnippetPanelComponent,
    QuerySnippetContainerComponent
  ],
  imports: [
    CommonModule,
    TuiAccordionModule,
    CovalentHighlightModule,
    TuiSelectModule,
    FormsModule,
    TuiDataListWrapperModule,
    TuiDataListModule,
    TuiInputModule,
    TuiButtonModule,
    TuiTextfieldControllerModule,
    TuiRootModule
  ]
})
export class QuerySnippetPanelModule {
}
