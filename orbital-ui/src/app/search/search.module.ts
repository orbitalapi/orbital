import {NgModule} from '@angular/core';
import {SearchResultComponent} from './seach-result/search-result.component';
import {CommonModule} from '@angular/common';
import {SearchResultListComponent} from './search-result-list/search-result-list.component';
import {SearchBarContainerComponent} from './search-bar/search-bar.container.component';
import {RouterModule} from '@angular/router';
import {TuiComboBoxModule, TuiDataListWrapperModule, TuiInputModule} from "@taiga-ui/kit";
import {TuiDataListModule, TuiTextfieldControllerModule} from "@taiga-ui/core";
import {FormsModule} from "@angular/forms";
import {TuiLetModule, TuiValueChangesModule} from "@taiga-ui/cdk";

@NgModule({
    imports: [CommonModule, RouterModule, TuiComboBoxModule, TuiTextfieldControllerModule, TuiInputModule, FormsModule, TuiDataListWrapperModule, TuiLetModule, TuiValueChangesModule, TuiDataListModule],
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
