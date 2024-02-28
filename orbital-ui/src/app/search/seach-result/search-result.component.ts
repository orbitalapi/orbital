import {Component, Input, OnInit} from '@angular/core';
import {SearchField, SearchResult} from '../search.service';
import {BaseSearchResultComponent} from './base-search-result-component';

@Component({
    selector: 'app-search-result',
    styleUrls: ['./search-result.component.scss'],
    template: `
      <div class="search-item">
        <div>
          <span class="type-name" [innerHtml]="name"></span>
          <span class="badge" [ngClass]="memberTypeForCSS(result)">{{ memberType(result) }}</span>
        </div>
        <div><span class="mono-badge fully-qualified-name" [innerHtml]="qualifiedName"></span></div>
        <p *ngIf="typeDoc" class="type-doc" [innerHtml]="typeDoc"></p>
      </div>
    `
})
export class SearchResultComponent extends BaseSearchResultComponent {

}
