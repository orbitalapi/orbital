import {Component, Input, OnInit} from '@angular/core';
import {SearchField, SearchResult} from '../search.service';

@Component({
  selector: 'app-search-result',
  styleUrls: ['./search-result.component.scss'],
  template: `
    <div class="search-result">
      <div class="type-name" [innerHtml]="name"></div>
      <div><span class="mono-badge fully-qualified-name" [innerHtml]="qualifiedName"></span></div>
      <p class="type-doc" [innerHtml]="typeDoc"></p>
    </div>
  `
})
export class SearchResultComponent {

  @Input()
  result: SearchResult;

  get name(): string {
    return this.getMatch('NAME') || this.result.qualifiedName.name;
  }

  get qualifiedName(): string {
    return this.getMatch('QUALIFIED_NAME') || this.result.qualifiedName.fullyQualifiedName;
  }

  get typeDoc(): string {
    return this.getMatch('TYPEDOC') || this.result.typeDoc;
  }

  private getMatch(searchField: SearchField): string | null {
    const match = this.result.matches.find(m => m.field === searchField);
    if (match) {
      return match.highlightedMatch;
    } else {
      return null;
    }
  }

}
