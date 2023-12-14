import {Component, Input, OnInit} from '@angular/core';
import {BaseGraphComponent} from '../../inheritence-graph/base-graph-component';
import {SchemaGraph, SchemaGraphNode, SchemaNodeSet} from '../../services/schema';
import {Observable} from 'rxjs/internal/Observable';
import {Subscription} from 'rxjs';
import {Router} from '@angular/router';
import {navigateForSearchResult} from '../../search/search-bar/search-bar.container.component';

/**
 * Service Lineage is theoretical lineage that's not captured at query time, but
 * published by services.  They state "I consume data from this other service".
 * This differs from our other LineageGraph, which shows traces of how data got into a query
 */
@Component({
  selector: 'app-service-lineage-graph',
  templateUrl: './service-lineage-graph.component.html',
  styleUrls: ['./service-lineage-graph.component.scss']
})
export class ServiceLineageGraphComponent extends BaseGraphComponent {

  schemaGraph: SchemaGraph = SchemaGraph.empty();
  schemaNodeSet: SchemaNodeSet = this.schemaGraph.toNodeSet();

  constructor(private router: Router) {
    super();
  }

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


  select($event: SchemaGraphNode) {
    if ($event.type === 'SERVICE') {
      this.router.navigate(['/services', $event.nodeId]);
    } else if ($event.type === 'OPERATION') {
      const parts: string[] = $event.nodeId.split('@@');
      this.router.navigate(['/services', parts[0], parts[1]]);
    }

  }

}
