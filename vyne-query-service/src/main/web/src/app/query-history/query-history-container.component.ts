import {Component, OnInit} from '@angular/core';
import {TypesService} from '../services/types.service';
import {ActivatedRoute, ParamMap} from '@angular/router';
import {map} from 'rxjs/operators';
import {Schema, Type} from '../services/schema';
import {buildInheritable, Inheritable} from '../inheritence-graph/inheritance-graph.component';

@Component({
  selector: 'app-type-viewer-container',
  template: `<app-query-history [queryResponseId]="queryResponseId"></app-query-history>`
})
export class QueryHistoryContainerComponent implements OnInit {
  constructor(private activeRoute: ActivatedRoute) {
  }

  public queryResponseId: string;

  ngOnInit() {
    this.activeRoute.paramMap.pipe(
      map((params: ParamMap) => params.get('queryResponseId'))
    ).subscribe(queryResponseId => {
      this.queryResponseId = queryResponseId;
    });
  }
}
