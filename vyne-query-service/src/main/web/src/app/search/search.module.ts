import {NgModule} from '@angular/core';
import {SearchBarComponent} from './search-bar/search-bar.component';
import {SearchResultComponent} from './seach-result/search-result.component';
import {NgSelectModule} from '@ng-select/ng-select';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {SearchResultListComponent} from './search-result-list/search-result-list.component';
import {SearchBarContainerComponent} from './search-bar/search-bar.container.component';
import {RouterModule} from '@angular/router';

@NgModule({
  imports: [NgSelectModule, CommonModule, BrowserModule, RouterModule],
  exports: [SearchBarContainerComponent,
    SearchBarComponent,
    SearchResultComponent,
    SearchResultListComponent],
  declarations: [SearchBarContainerComponent,
    SearchBarComponent,
    SearchResultComponent,
    SearchResultListComponent],
  providers: [],
})
export class SearchModule {
}
