import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FormControl} from '@angular/forms';
import {Observable} from 'rxjs';
import {map, startWith} from 'rxjs/operators';
import {isNullOrUndefined} from 'util';
import {falseIfMissing} from 'protractor/built/util';
import {MatAutocompleteSelectedEvent} from '@angular/material/autocomplete';
import {equals, FilterOperation} from './filter-operations';


export interface FilterItem {
  readonly subject: any | null;
  readonly operation: FilterOperation | null;
  readonly criteria: any | null;
}

export enum CriteriaType {
  Text = 'Text',
  Numeric = 'Numeric',
  Enum = 'Enum'
}

export interface SubjectFilterOptions {
  operations: FilterOperation[];
  allowMultipleSelect: boolean;
  criteriaType: CriteriaType;
  options: any[] | null;
  optionDisplayFunction: (value: any) => string | null;
}

@Component({
  selector: 'app-filter-item',
  template: `
    <div class="filter-item">
      <div class="subject-select">
        <mat-form-field appearance="outline">
          <mat-label>{{subjectLabel}}</mat-label>
          <input type="text"
                 matInput
                 [formControl]="subjectFilterInput"
                 [matAutocomplete]="subjectAuto">
          <mat-autocomplete #subjectAuto="matAutocomplete" [displayWith]="subjectDisplayWith"
                            (optionSelected)="subjectSelected($event)"
          >
            <mat-option *ngFor="let subjectOption of filteredSubjects | async"
                        [value]="subjectOption"> {{ subjectDisplayWith(subjectOption)}}</mat-option>
          </mat-autocomplete>
        </mat-form-field>
      </div>
      <div class="operation-select" *ngIf="filterOptions">
        <mat-form-field appearance="outline">
          <mat-label>Operation</mat-label>
          <mat-select [value]="filterItem.operation" (valueChange)="filterOperationUpdated($event)">
            <mat-option *ngFor="let operation of filterOptions.operations" [value]="operation">
              {{operation.symbol}}
            </mat-option>
          </mat-select>
        </mat-form-field>
      </div>
      <div class="criteria-select" *ngIf="filterOptions" [ngSwitch]="filterOptions.criteriaType">
        <mat-form-field appearance="outline" *ngSwitchCase="criteriaType.Text">
          <mat-label>Value</mat-label>
          <input matInput placeholder="Value">
        </mat-form-field>
      </div>
    </div>
  `,
  styleUrls: ['./filter-item.component.scss']
})
export class FilterItemComponent implements OnInit {
  criteriaType = CriteriaType; // workaround for ngSwitchCase with enums
  filteredSubjects: Observable<any[]>;

  subjectFilterInput = new FormControl();
  subjectLabel: string = 'Filter';

  @Input()
  filterItem: FilterItem;

  @Input()
  filterItemIndex: number;

  @Output()
  filterUpdated = new EventEmitter<FilterItem>();

  @Input()
  subjects: any[];

  @Input()
  subjectFilter: ((candidates: any[], input: string) => any[]) | null;

  @Input()
  subjectDisplayWith: ((value: any) => string) | null = this.defaultDisplayFunction;

  @Input()
  filterOptionsProvider: ((subject: any) => SubjectFilterOptions) = this.defaultFilterOptionsProvider;

  private selectedSubject: any;
  filterOptions: SubjectFilterOptions;

  ngOnInit(): void {
    this.filteredSubjects = this.subjectFilterInput.valueChanges
      .pipe(
        startWith(''),
        map(value => {
          if (isNullOrUndefined(this.subjectFilter)) {
            return this.subjects;
          } else {
            return this.subjectFilter(this.subjects, value);
          }

        })
      );
  }

  defaultDisplayFunction(input): string {
    return input;
  }

  defaultFilterOptionsProvider(input: any): SubjectFilterOptions {
    return {
      allowMultipleSelect: false,
      criteriaType: CriteriaType.Text,
      operations: [equals],
      optionDisplayFunction: null,
      options: null
    };
  }

  subjectSelected($event: MatAutocompleteSelectedEvent) {
    this.selectedSubject = $event.option.value;
    this.filterOptions = this.filterOptionsProvider(this.selectedSubject);
    this.filterItem = {
      ...this.filterItem,
      operation: this.filterOptions.operations[0],
      subject: this.selectedSubject
    };

  }

  filterOperationUpdated($event: FilterOperation) {
    this.filterItem = {
      ...this.filterItem,
      operation: $event
    };
  }
}
