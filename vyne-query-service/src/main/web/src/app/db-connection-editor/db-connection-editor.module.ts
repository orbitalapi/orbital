import {NgModule} from '@angular/core';

import {DbConnectionEditorComponent} from './db-connection-editor.component';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {MatCardModule} from '@angular/material/card';
import {MatSelectModule} from '@angular/material/select';
import {MatInputModule} from '@angular/material/input';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';

@NgModule({
  imports: [CommonModule, BrowserModule, BrowserAnimationsModule, MatCardModule, MatSelectModule, MatInputModule, FormsModule, MatButtonModule],
  exports: [DbConnectionEditorComponent],
  declarations: [DbConnectionEditorComponent],
  providers: [],
})
export class DbConnectionEditorModule {
}
