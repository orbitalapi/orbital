import {NgModule} from '@angular/core';
import {AttributeTableComponent} from './attribute-table.component';
import {RouterModule} from '@angular/router';
import {DescriptionEditorModule} from '../description-editor/description-editor.module';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';


@NgModule({
  imports: [BrowserModule, CommonModule, RouterModule, DescriptionEditorModule],
  exports: [AttributeTableComponent],
  declarations: [AttributeTableComponent],
  providers: [],
})
export class AttributeTableModule {
}
