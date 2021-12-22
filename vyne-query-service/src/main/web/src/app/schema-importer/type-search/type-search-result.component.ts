import {Component, ElementRef, HostBinding, OnInit} from '@angular/core';
import {BaseSearchResultComponent} from '../../search/seach-result/base-search-result-component';
import {FocusableOption, FocusOrigin, Highlightable} from '@angular/cdk/a11y';

@Component({
  selector: 'app-type-search-result',
  // see https://indepth.dev/posts/1147/doing-a11y-easily-with-angular-cdk-keyboard-navigable-lists
  host: {
    tabindex: '-1',
    role: 'list-item',
  },
  template: `
    <div class="search-result" [ngClass]="{ active: isActive } ">
      <div class="type-name" [innerHtml]="name"></div>
      <div><span class="mono-badge extra-small fully-qualified-name" [innerHtml]="qualifiedName"></span></div>
      <p class="type-doc" [innerHtml]="typeDoc"></p>
    </div>
  `,
  styleUrls: ['./type-search-result.component.scss']
})
export class TypeSearchResultComponent extends BaseSearchResultComponent implements Highlightable {
  isActive = false;

  constructor(private element:ElementRef) {
    super();
  }

  getLabel(): string {
    return '';
  }

  setActiveStyles(): void {
    this.isActive = true;
  }

  setInactiveStyles(): void {
    this.isActive = false;
  }


}
