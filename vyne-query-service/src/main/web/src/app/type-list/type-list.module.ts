import {NgModule} from '@angular/core';
import {TypeListComponent} from './type-list.component';
import {MatToolbarModule} from '@angular/material/toolbar';
import {SearchModule} from '../search/search.module';
import {CommonModule} from '@angular/common';
import {MatButtonModule} from '@angular/material/button';
import {FilterTypesComponent} from './filter-types/filter-types.component';
import {CovalentHighlightModule} from '@covalent/highlight';
import {ReactiveFormsModule} from '@angular/forms';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatSelectModule} from '@angular/material/select';
import {MatCardModule} from '@angular/material/card';
import {MatInputModule} from '@angular/material/input';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {MatIconModule} from '@angular/material/icon';
import {HeaderBarModule} from '../header-bar/header-bar.module';
import {ScrollingModule} from '@angular/cdk/scrolling';

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
    ScrollingModule
  ],
  exports: [TypeListComponent],
  declarations: [TypeListComponent, FilterTypesComponent],
  providers: [],
})
export class TypeListModule {
}
