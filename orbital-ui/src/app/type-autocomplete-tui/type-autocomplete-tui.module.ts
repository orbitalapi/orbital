import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {TypeAutocompleteTuiComponent} from "./type-autocomplete-tui.component";
import {TuiComboBoxModule, TuiDataListWrapperModule, TuiFilterByInputPipeModule, TuiSelectModule} from "@taiga-ui/kit";
import {TuiDataListModule, TuiTextfieldControllerModule} from "@taiga-ui/core";
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
    TuiDataListModule,
    TuiDataListWrapperModule,
    TuiFilterByInputPipeModule,
    TuiSelectModule,
    TuiTextfieldControllerModule,
    FormsModule
  ]
})
export class TypeAutocompleteTuiModule { }
