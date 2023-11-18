import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {UntypedFormControl} from '@angular/forms';
import {SearchDates, today, yesterday} from './search-dates';


@Component({
  selector: 'app-cask-ingestion-errors-search-panel',
  templateUrl: './cask-ingestion-errors-search-panel.component.html',
  styleUrls: ['./cask-ingestion-errors-search-panel.component.scss']
})
export class CaskIngestionErrorsSearchPanelComponent implements OnInit {
  searchEndDate: UntypedFormControl;
  searchStartDate: UntypedFormControl;
  @Output() searchCriteriaChanged = new EventEmitter<SearchDates>();

  constructor() {
    this.searchEndDate = new UntypedFormControl(today());
    this.searchStartDate = new UntypedFormControl(yesterday());
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
