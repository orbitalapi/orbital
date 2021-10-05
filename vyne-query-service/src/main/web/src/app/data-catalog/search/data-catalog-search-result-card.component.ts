import {Component, Input, OnInit} from '@angular/core';
import {SearchResult} from '../../search/search.service';
import {Metadata, fqn} from '../../services/schema';
import {DATA_OWNER_FQN, DATA_OWNER_TAG_OWNER_NAME} from '../data-catalog.models';

@Component({
  selector: 'app-data-catalog-search-result-card',
  template: `
    <div class="row">
      <span class="matched-name" [matTooltip]="toolTip()">{{ shortDisplayName() }}</span>
    </div>
    <div class="row">
      <span class="mono-badge small">{{ searchResult.qualifiedName.longDisplayName }}</span>
      <span class="metadata" *ngFor="let metadata of otherMetadata">{{ metadata.name.shortDisplayName }}</span>
    </div>
    <div class="row row-spacer key-value-pair" *ngIf="owner">
      <span class="key">Data owner:</span>
      <span class="value">{{owner.params[dataTagOwnerName]}}</span>
    </div>
    <div class="row">
      <markdown [data]="searchResult.typeDoc | slice:0:500"></markdown>
    </div>
  `,
  styleUrls: ['./data-catalog-search-result-card.component.scss']
})
export class DataCatalogSearchResultCardComponent {

  private _searchResult: SearchResult;
  owner: Metadata;
  otherMetadata: Metadata[];

  // To workaround angular template issues;
  dataTagOwnerName = DATA_OWNER_TAG_OWNER_NAME;

  @Input()
  get searchResult(): SearchResult {
    return this._searchResult;
  }

  set searchResult(value) {
    if (this._searchResult === value) {
      return;
    }
    this._searchResult = value;

    if (this.searchResult && this.searchResult.metadata) {
      const metadata = this.searchResult.metadata.concat();
      this.owner = metadata.find(m => m.name.fullyQualifiedName === DATA_OWNER_FQN);
      this.otherMetadata = metadata.filter(m => m.name.fullyQualifiedName !== DATA_OWNER_FQN);
    } else {
      this.owner = null;
      this.otherMetadata = [];
    }


  }

  shortDisplayName() {
    if (this.searchResult.matchedFieldName) {
      const parts = this.searchResult.matchedFieldName.split(':');
      const fullyQualifiedName = fqn(parts[0]);
      return `${fullyQualifiedName.name}.${parts[1]}`;
    } else {
      return this.searchResult.qualifiedName.shortDisplayName;
    }
  }

  toolTip() {
    if (this.searchResult.matchedFieldName) {
      const parts = this.searchResult.matchedFieldName.split(':');
      const fullyQualifiedName = fqn(parts[0]);
      return `Field ${parts[1]} on ${parts[0]}`;
    } else {
      return this.searchResult.qualifiedName.fullyQualifiedName;
    }
  }
}
