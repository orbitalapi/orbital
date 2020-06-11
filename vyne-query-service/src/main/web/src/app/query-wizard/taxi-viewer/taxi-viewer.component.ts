import {Component, Input, OnInit} from '@angular/core';
import {Fact, Query, QueryMode, QueryResult, QueryService} from './query.service';
import {SchemaWithTaxi, TypesService} from './types.service';

@Component({
  selector: 'app-taxi-viewer',
  templateUrl: './taxi-viewer.component.html',
  styleUrls: ['./taxi-viewer.component.scss'],
  preserveWhitespaces: true,
})
export class TaxiViewerComponent implements OnInit {

  @Input()
  membersToInclude: string[];

  @Input()
  facts: Fact[];

  @Input()
  targetType: string;

  @Input()
  queryMode: QueryMode;

  expanded = true;

  loading = false;

  taxiComponentExpanded = true;
  vyneQueryExpanded = true;

  constructor(private schemaService: TypesService, private queryService: QueryService) {
  }

  get taxi(): string {
    if (this.schemaWithTaxi) {
      return this.schemaWithTaxi.taxi;
    } else {
      return '';
    }
  }

  schemaWithTaxi: SchemaWithTaxi;
  queryResult: QueryResult;

  get result(): string {
    if (!this.queryResult || !this.queryResult.results) { return ''; }
    return JSON.stringify(this.queryResult.results, null, 2);
  }


  ngOnInit(): void {
    this.loadSchema();
  }

  private loadSchema() {
    if (!this.membersToInclude) {
      console.error('You must specify the members to include for this component');
      return;
    }
    this.schemaService.getTaxiForMembers(this.membersToInclude)
      .subscribe(result => {
        this.schemaWithTaxi = result;
      });
  }

  submitQuery(event) {
    event.stopImmediatePropagation();
    event.stopPropagation();
    this.expanded = true;
    this.loading = true;
    const query = new Query(this.targetType, this.facts, this.queryMode);
    this.queryService.submitQuery(query)
      .subscribe(result => {
        console.log(result);
        this.queryResult = result;
        this.loading = false;
        this.vyneQueryExpanded = false;
        this.taxiComponentExpanded = false;
      });

  }
}
