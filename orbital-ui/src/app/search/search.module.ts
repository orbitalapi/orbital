import { TuiTextfieldControllerModule, TuiComboBoxModule, TuiInputModule } from "@taiga-ui/legacy";
import {NgModule} from '@angular/core';
import {MarkdownModule} from 'ngx-markdown';
import {SearchResultComponent} from './seach-result/search-result.component';
import {CommonModule} from '@angular/common';
import {SearchResultListComponent} from './search-result-list/search-result-list.component';
import {SearchBarContainerComponent} from './search-bar/search-bar.container.component';
import {RouterModule} from '@angular/router';
import { TuiDataListWrapper } from "@taiga-ui/kit";
import { TuiDataList } from "@taiga-ui/core";
import {FormsModule} from "@angular/forms";
import { TuiValueChanges, TuiLet } from "@taiga-ui/cdk";

@NgModule({
  imports: [
    CommonModule,
    RouterModule,
    TuiComboBoxModule,
    TuiTextfieldControllerModule,
    TuiInputModule,
    FormsModule,
    ...TuiDataListWrapper,
    TuiLet,
    TuiValueChanges,
    ...TuiDataList,
    MarkdownModule.forRoot()
  ],
    exports: [SearchBarContainerComponent,
        SearchResultComponent,
        SearchResultListComponent],
    declarations: [SearchBarContainerComponent,
        SearchResultComponent,
        SearchResultListComponent],
    providers: [],
})
export class SearchModule {
}
