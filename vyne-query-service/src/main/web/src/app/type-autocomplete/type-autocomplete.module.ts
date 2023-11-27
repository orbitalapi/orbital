import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TypeAutocompleteComponent} from './type-autocomplete.component';
import {MatLegacyFormFieldModule as MatFormFieldModule} from '@angular/material/legacy-form-field';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatLegacyAutocompleteModule as MatAutocompleteModule} from '@angular/material/legacy-autocomplete';
import {MatIconModule} from '@angular/material/icon';
import {MatLegacyChipsModule as MatChipsModule} from '@angular/material/legacy-chips';
import {MatLegacyInputModule as MatInputModule} from '@angular/material/legacy-input';
import {SchemaMemberAutocompleteComponent} from './schema-member-autocomplete.component';
import {ConnectionNameAutocompleteComponent} from './connection-name-autocomplete.component';

@NgModule({
  declarations: [
    TypeAutocompleteComponent,
    SchemaMemberAutocompleteComponent,
    ConnectionNameAutocompleteComponent
  ],
  imports: [
    CommonModule,

    FormsModule,
    ReactiveFormsModule,

    MatFormFieldModule,
    MatInputModule,
    MatAutocompleteModule,
    MatIconModule,
    MatChipsModule
  ],
  exports: [TypeAutocompleteComponent, SchemaMemberAutocompleteComponent, ConnectionNameAutocompleteComponent]
})
export class TypeAutocompleteModule {
}
