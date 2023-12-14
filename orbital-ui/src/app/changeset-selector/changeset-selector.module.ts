import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ChangesetSelectorComponent} from './changeset-selector.component';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatLegacySelectModule as MatSelectModule} from '@angular/material/legacy-select';
import {ChangesetService} from "./changeset.service";


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
  providers: [
    ChangesetService
  ]
})
export class ChangesetSelectorModule {
}
