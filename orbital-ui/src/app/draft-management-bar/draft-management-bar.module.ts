import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatLegacySnackBarModule } from '@angular/material/legacy-snack-bar';
import { MatLegacyTooltipModule } from '@angular/material/legacy-tooltip';
import { MatToolbarModule } from '@angular/material/toolbar';
import { TuiButtonModule } from '@taiga-ui/core';
import {DraftManagementBarComponent} from "./draft-management-bar.component";
import {ChangesetSelectorModule} from "../changeset-selector/changeset-selector.module";



@NgModule({
  declarations: [DraftManagementBarComponent],
  exports: [DraftManagementBarComponent],
  imports: [
    CommonModule,
    ChangesetSelectorModule,
    MatToolbarModule,
    MatIconModule,
    MatLegacyTooltipModule,
    TuiButtonModule,
    MatLegacySnackBarModule
  ],

})
export class DraftManagementBarModule { }
