import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
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
    MatTooltipModule,
    TuiButtonModule,
    MatSnackBarModule
  ],

})
export class DraftManagementBarModule { }
