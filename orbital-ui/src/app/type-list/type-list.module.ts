import { NgModule } from '@angular/core';
import { TypeListComponent } from './type-list.component';
import { MatToolbarModule } from '@angular/material/toolbar';
import { SearchModule } from '../search/search.module';
import { CommonModule } from '@angular/common';
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { FilterTypesComponent } from './filter-types/filter-types.component';
import { CovalentHighlightModule } from '@covalent/highlight';
import { ReactiveFormsModule } from '@angular/forms';
import { MatLegacyFormFieldModule as MatFormFieldModule } from '@angular/material/legacy-form-field';
import { MatLegacySelectModule as MatSelectModule } from '@angular/material/legacy-select';
import { MatLegacyCardModule as MatCardModule } from '@angular/material/legacy-card';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
import { TypeAutocompleteModule } from '../type-autocomplete/type-autocomplete.module';
import { MatIconModule } from '@angular/material/icon';
import { HeaderBarModule } from '../header-bar/header-bar.module';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { ScrollingModule as ExperimentalScrollingModule } from '@angular/cdk-experimental/scrolling';
import { TuiGroupModule, TuiTextfieldControllerModule } from '@taiga-ui/core';
import { TuiCheckboxBlockModule, TuiInputModule } from '@taiga-ui/kit';
import { ChangesetSelectorModule } from '../changeset-selector/changeset-selector.module';

@NgModule({
  imports: [
    MatToolbarModule,
    MatButtonModule,
    SearchModule,
    CommonModule,
    CovalentHighlightModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatCardModule,
    MatInputModule,
    TypeAutocompleteModule,
    MatIconModule,
    HeaderBarModule,
    ScrollingModule,
    ExperimentalScrollingModule,
    TuiGroupModule,
    TuiCheckboxBlockModule,
    TuiInputModule,
    TuiTextfieldControllerModule,
    ChangesetSelectorModule,
  ],
  exports: [TypeListComponent, FilterTypesComponent],
  declarations: [TypeListComponent, FilterTypesComponent],
  providers: [],
})
export class TypeListModule {
}
