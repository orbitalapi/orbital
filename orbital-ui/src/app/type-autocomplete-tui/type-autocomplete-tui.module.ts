import { TuiTextfieldControllerModule, TuiComboBoxModule, TuiSelectModule } from "@taiga-ui/legacy";
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {TypeAutocompleteTuiComponent} from "./type-autocomplete-tui.component";
import { TuiDataListWrapper, TuiFilterByInputPipe } from "@taiga-ui/kit";
import { TuiDataList } from "@taiga-ui/core";
import {FormsModule} from "@angular/forms";



@NgModule({
  declarations: [
    TypeAutocompleteTuiComponent
  ],
  exports: [
    TypeAutocompleteTuiComponent
  ],
  imports: [
    CommonModule,
    TuiComboBoxModule,
    ...TuiDataList,
    ...TuiDataListWrapper,
    TuiFilterByInputPipe,
    TuiSelectModule,
    TuiTextfieldControllerModule,
    FormsModule
  ]
})
export class TypeAutocompleteTuiModule { }
