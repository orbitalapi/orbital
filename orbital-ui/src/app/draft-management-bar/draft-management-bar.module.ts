import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {DraftManagementBarComponent} from "./draft-management-bar.component";
import {ChangesetSelectorModule} from "../changeset-selector/changeset-selector.module";



@NgModule({
  declarations: [DraftManagementBarComponent],
  exports: [DraftManagementBarComponent],
  imports: [
    CommonModule,
    ChangesetSelectorModule
  ],

})
export class DraftManagementBarModule { }
