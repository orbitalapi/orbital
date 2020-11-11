import {NgModule} from '@angular/core';

import {FilterBarComponent} from './filter-bar.component';
import {FilterItemComponent} from './filter-item.component';
import {MatAutocompleteModule} from '@angular/material/autocomplete';
import {ReactiveFormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';
import {MatButtonModule} from '@angular/material/button';
import {Type} from '../services/schema';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';

@NgModule({
  imports: [MatAutocompleteModule, ReactiveFormsModule, CommonModule, MatButtonModule, MatInputModule, MatSelectModule],
  exports: [FilterBarComponent],
  declarations: [FilterBarComponent, FilterItemComponent],
  providers: [],
})
export class FilterBarModule {
}
