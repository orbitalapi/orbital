import {Component, Input, OnInit} from '@angular/core';
import {SearchDates, today, yesterday} from './cask-ingestion-errors-search-panel/search-dates';
import {CaskConfigRecord} from '../../services/cask.service';
import {SearchInput} from './cask-ingestion-errors-grid/search-input';

@Component({
  selector: 'app-cask-ingestion-errors',
  templateUrl: './cask-ingestion-errors.component.html',
  styleUrls: ['./cask-ingestion-errors.component.scss']
})
export class CaskIngestionErrorsComponent implements OnInit {
  private _caskConfigRecord: CaskConfigRecord;

  @Input()
  set caskConfigRecord(value: CaskConfigRecord) {
    if (value) {
      this._caskConfigRecord = value;
      this.searchInput = {
        searchEnd:  this.searchInput && this.searchInput.searchEnd ? this.searchInput.searchEnd : today(),
        searchStart: this.searchInput && this.searchInput.searchStart ? this.searchInput.searchStart : yesterday(),
        tableName: value.tableName
      };
    }
  }

  searchInput: SearchInput;
  constructor() { }
  ngOnInit() {
  }

  doSearch(searchCriteria: SearchDates) {
    console.log(`Search Date update search input is ${JSON.stringify(this.searchInput)}`);
    this.searchInput = {
      searchEnd: searchCriteria.searchEnd,
      searchStart: searchCriteria.searchStart,
      tableName: this._caskConfigRecord.tableName
    };
  }
}
