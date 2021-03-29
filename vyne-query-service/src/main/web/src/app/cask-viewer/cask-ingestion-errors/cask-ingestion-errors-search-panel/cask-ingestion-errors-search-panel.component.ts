import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {FormControl} from '@angular/forms';
import {SearchDates, today, yesterday} from './search-dates';


@Component({
  selector: 'app-cask-ingestion-errors-search-panel',
  templateUrl: './cask-ingestion-errors-search-panel.component.html',
  styleUrls: ['./cask-ingestion-errors-search-panel.component.scss']
})
export class CaskIngestionErrorsSearchPanelComponent implements OnInit {
  searchEndDate: FormControl;
  searchStartDate: FormControl;
  @Output() searchCriteriaChanged = new EventEmitter<SearchDates>();

  constructor() {
    this.searchEndDate = new FormControl(today());
    this.searchStartDate = new FormControl(yesterday());
  }

  ngOnInit() {
  }

  onSearchCriteriaUpdated() {
    this.searchCriteriaChanged.emit({
      searchStart: this.searchStartDate.value,
      searchEnd: this.searchEndDate.value
    });
  }
}
