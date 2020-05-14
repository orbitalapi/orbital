import {Component, Input, OnInit} from '@angular/core';
import {SourceCompilationError} from '../services/schema';

@Component({
  selector: 'app-error-list',
  styleUrls: ['./error-list.component.scss'],
  template: `
    <div class="header">
      Problems
    </div>
    <div *ngFor="let error of errors">
      <div class="row">
        <span class="errorMessage">{{ error.detailMessage }}</span>
        <span class="position">[{{error.line}},{{error.char}}]</span>
      </div>
    </div>
  `
})
export class ErrorListComponent {

  @Input()
  errors: SourceCompilationError[];

}
