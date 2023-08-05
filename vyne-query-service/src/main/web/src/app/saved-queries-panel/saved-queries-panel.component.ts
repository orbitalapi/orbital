import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {TypesService} from "../services/types.service";
import {map} from "rxjs/operators";
import {SavedQuery} from "../services/type-editor.service";
import {Observable} from "rxjs/internal/Observable";

@Component({
  selector: 'app-saved-queries-panel',
  template: `
    <div>
      <app-saved-query (click)="savedQueryClicked.emit(query)" *ngFor="let query of queries$ | async" [query]="query"></app-saved-query>
    </div>
  `,
  styleUrls: ['./saved-queries-panel.component.scss']
})
export class SavedQueriesPanelComponent {
  queries$: Observable<SavedQuery[]>;

  constructor(private typeService: TypesService) {
    this.queries$ = typeService.getTypes()
      .pipe(map(schema => schema.queries))
  }

  @Output()
  savedQueryClicked = new EventEmitter<SavedQuery>()


}
