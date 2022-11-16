import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TuiDataListModule, TuiSvgModule } from '@taiga-ui/core';
import { ChangesetSelectorComponent } from './changeset-selector.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { TuiSelectModule } from '@taiga-ui/kit';
import { MatSelectModule } from '@angular/material/select';


@NgModule({
  declarations: [
    ChangesetSelectorComponent,
  ],
  exports: [
    ChangesetSelectorComponent,
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatSelectModule
  ],
})
export class ChangesetSelectorModule {
}
