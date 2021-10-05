import {Component, Input, OnInit} from '@angular/core';
import {BaseGraphComponent} from '../../inheritence-graph/base-graph-component';
import {SchemaGraph, SchemaNodeSet} from '../../services/schema';
import {Observable} from 'rxjs/internal/Observable';
import {Subscription} from 'rxjs';

@Component({
  selector: 'app-lineage-graph',
  templateUrl: './lineage-graph.component.html',
  styleUrls: ['./lineage-graph.component.scss']
})
export class LineageGraphComponent extends BaseGraphComponent {

  schemaGraph: SchemaGraph = SchemaGraph.empty();
  schemaNodeSet: SchemaNodeSet = this.schemaGraph.toNodeSet();

  private schemaSubscription: Subscription;
  private _schemaGraph$: Observable<SchemaGraph>;

  @Input()
  get schemaGraph$(): Observable<SchemaGraph> {
    return this._schemaGraph$;
  }

  set schemaGraph$(value: Observable<SchemaGraph>) {
    if (this.schemaSubscription) {
      this.schemaSubscription.unsubscribe();
      this.schemaGraph = SchemaGraph.empty();
    }
    this._schemaGraph$ = value;
    if (this.schemaGraph$) {
      this.schemaSubscription = this.schemaGraph$.subscribe(schemaGraph => {
        this.appendSchemaGraph(schemaGraph);
      });
    }
  }

  private appendSchemaGraph(schemaGraph: SchemaGraph) {
    this.schemaGraph.add(schemaGraph);
    this.schemaNodeSet = this.schemaGraph.toNodeSet();

    // console.log('Updated typeLinks.  Now:');
    // console.log(JSON.stringify(this.typeLinks));
  }


  select($event: any) {

  }

}
