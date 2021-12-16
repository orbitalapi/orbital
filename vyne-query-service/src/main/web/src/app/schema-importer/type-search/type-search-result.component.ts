import { Component, OnInit } from '@angular/core';
import {BaseSearchResultComponent} from '../../search/seach-result/base-search-result-component';

@Component({
  selector: 'app-type-search-result',
  template: `
    <div class="search-result">
      <div class="type-name" [innerHtml]="name"></div>
      <div><span class="mono-badge extra-small fully-qualified-name" [innerHtml]="qualifiedName"></span></div>
      <p class="type-doc" [innerHtml]="typeDoc"></p>
    </div>
  `,
  styleUrls: ['./type-search-result.component.scss']
})
export class TypeSearchResultComponent extends BaseSearchResultComponent {

}
