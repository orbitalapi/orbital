import {Input} from '@angular/core';
import {SearchField, SearchResult} from '../search.service';

export class BaseSearchResultComponent {
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
