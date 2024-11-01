import { TuiTextfieldControllerModule, TuiInputModule, TuiSelectModule } from "@taiga-ui/legacy";
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { QuerySnippetPanelComponent } from './query-snippet-panel.component';
import { TuiDataListWrapper, TuiAccordion } from '@taiga-ui/kit';
import { CovalentHighlightModule } from '@covalent/highlight';
import { QuerySnippetContainerComponent } from './query-snippet-container.component';
import { FormsModule } from '@angular/forms';
import { TuiRoot, TuiDataList, TuiButton } from '@taiga-ui/core';


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
    ...TuiAccordion,
    CovalentHighlightModule,
    TuiSelectModule,
    FormsModule,
    ...TuiDataListWrapper,
    ...TuiDataList,
    TuiInputModule,
    TuiButton,
    TuiTextfieldControllerModule,
  ]
})
export class QuerySnippetPanelModule {
}
