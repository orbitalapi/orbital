import {Component, OnInit} from '@angular/core';
import * as shape from 'd3-shape';
import {AlertsService, ItemsService, ProductsService} from '../../services';
import {HttpClient} from '@angular/common/http'
import {Subject} from 'rxjs';
import {environment} from '../../environments/environment';
import {colorSets} from '@swimlane/ngx-charts-dag/src/utils'

@Component({
  selector: 'qs-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  viewProviders: [ItemsService, ProductsService, AlertsService],
})
export class DashboardComponent implements OnInit {
  constructor(private http: HttpClient) {
  }

  // Graph config
  // curve: any = shape.curveLinear;
  curve = shape.curveBundle.beta(1);
  taxiDef: string = '';
  showLegend:boolean = true;
  taxiDefUpdates: Subject<string> = new Subject();
  errors:Array<any> = [];
  graphData: any = {nodes:[], links:[]};
  colorScheme: any = null;

  view: any[];
  fitContainer: boolean = true;
  autoZoom: boolean = true;
  
  onTaxiDefChanged(): void {
    this.taxiDefUpdates.next(this.taxiDef)
  }

  ngOnInit(): void {
    this.colorScheme = colorSets.find(s => {
      return s.name === 'picnic'
    });

    // TODO :  should be throttle(Observable.interval(500)), but that's not working.
    this.taxiDefUpdates.throttleTime(500)
      .subscribe(taxiDef => {
        this.http.post(`${environment.apiUrl}/schemas/taxi-graph`, taxiDef)
          .subscribe(data => {
            this.errors = [];
            this.graphData = data;
          }, errorResponse => {
            // compilation exceptions
            if (errorResponse.status == 406) {
              this.errors = errorResponse.error.errors
            }
          });
        console.debug(taxiDef)
      })
  }

}
