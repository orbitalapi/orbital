import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FilterItem} from './filter-item.component';

@Component({
  selector: 'app-filter-bar',
  styleUrls: ['./filter-bar.component.scss'],
  template: `
    <div class="filter-bar-background">
      <div *ngFor="let filter of filters; let i = index" class="filter-item">
        <img src="assets/img/clear-cross-circle.svg" class="img-button" (click)="removeFilter(i)">
        <app-filter-item [filterItemIndex]="i"
                         [subjects]="subjects"
                         [subjectFilter]="subjectFilter"
                         [subjectDisplayWith]="subjectDisplayWith"
        >
        </app-filter-item>
      </div>
      <button mat-stroked-button (click)="addFilter()">Add</button>
    </div>
  `
})
export class FilterBarComponent {
  @Input()
  subjects: any[];
  @Input()
  subjectFilter: ((candidates: any[], input: string) => any[]) | null;
  @Input()
  subjectDisplayWith: ((value: any) => string) | null;

  @Input()
  filters: FilterItem[] = [];

  @Output()
  filtersChanged = new EventEmitter<FilterItem[]>();

  addFilter() {
    this.filters.push({
      subject: null,
      criteria: null,
      operation: null
    });
  }

  removeFilter(i: number) {
    this.filters.splice(i, 1);
  }
}
