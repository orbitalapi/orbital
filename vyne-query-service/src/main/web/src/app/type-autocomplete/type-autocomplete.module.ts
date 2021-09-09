import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TypeAutocompleteComponent} from './type-autocomplete.component';
import {MatFormFieldModule} from '@angular/material/form-field';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatAutocompleteModule} from '@angular/material/autocomplete';
import {MatIconModule} from '@angular/material/icon';
import {MatChipsModule} from '@angular/material/chips';
import {MatInputModule} from '@angular/material/input';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {SchemaMemberAutocompleteComponent} from './schema-member-autocomplete.component';

@NgModule({
  declarations: [
    TypeAutocompleteComponent,
    SchemaMemberAutocompleteComponent
  ],
  imports: [
    CommonModule,
    BrowserAnimationsModule,

    FormsModule,
    ReactiveFormsModule,

    MatFormFieldModule,
    MatInputModule,
    MatAutocompleteModule,
    MatIconModule,
    MatChipsModule
  ],
  exports: [TypeAutocompleteComponent, SchemaMemberAutocompleteComponent]
})
export class TypeAutocompleteModule {
}
