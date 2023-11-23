import {ChangeDetectionStrategy, ChangeDetectorRef, Component} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {TypesService} from "../services/types.service";
import {Observable} from "rxjs";
import {map, mergeMap} from "rxjs/operators";
import {SavedQuery} from "../services/type-editor.service";
import {MetricsService, StreamMetricsData} from "../services/metrics.service";
import {
    ApexAxisChartSeries,
    ApexChart,
    ApexDataLabels,
    ApexFill,
    ApexStroke,
    ApexXAxis,
    ApexYAxis
} from "ng-apexcharts";

@Component({
    selector: 'app-endpoint-monitor',
    template: `
        <app-header-component-layout *ngIf="query$ | async as query" [title]="query?.name.name">

            <div>
                <div class="">
                    <app-panel-header title="Metrics"></app-panel-header>
                    <apx-chart *ngIf="chartSeries" [chart]="chartConfig" [series]="chartSeries"
                               [stroke]="stroke"
                               [fill]="fill"
                               [yaxis]="numericYAxis"
                               [dataLabels]="dataLabels"
                               [xaxis]="dateTimeXAxis"></apx-chart>
                </div>
                <div class="">
                    <app-panel-header title="Source"></app-panel-header>
                    <app-code-viewer [sources]="query.sources"></app-code-viewer>
                </div>
            </div>
        </app-header-component-layout>
    `,
    styleUrls: ['./endpoint-monitor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EndpointMonitorComponent {

    query$: Observable<SavedQuery>
    metrics$: Observable<StreamMetricsData>

    dataLabels: ApexDataLabels = {
        enabled: false
    }

    stroke: ApexStroke = {
        width: 2,
        curve: "straight"
    }

    fill:ApexFill =  {
        type: "gradient",
        gradient: {
            shadeIntensity: 1,
            opacityFrom: 0.7,
            opacityTo: 0.9,
            stops: [0, 90, 100]
        }
    }
    chartSeries: ApexAxisChartSeries;
    dateTimeXAxis : ApexXAxis = {
        type: "datetime"
    }
    numericYAxis : ApexYAxis = {
        labels: {
            formatter: val => { return val.toFixed(0) }
        }
    }


    chartConfig: ApexChart = {
        type: 'area',
        height: 200,

        zoom: {
            type: "x",
            enabled: true,
            autoScaleYaxis: true
        },
        toolbar: {
            autoSelected: "zoom"
        }
    }

    constructor(
        private activatedRoute: ActivatedRoute,
        private typeService: TypesService,
        private metricsService: MetricsService,
        private changeDetector: ChangeDetectorRef
    ) {

        const endpointName$ = activatedRoute.paramMap.pipe(
            map(paramMap => {
                const endpoint = paramMap.get('endpointName');
                return endpoint
            }))

        this.query$ = endpointName$.pipe(
            mergeMap(endpoint => {
                return typeService.getQuery(endpoint)
            })
        )
        this.metrics$ = endpointName$.pipe(
            mergeMap(endpoint => {
                return metricsService.getMetricsForStream(endpoint)
            })
        )
        this.metrics$.subscribe(metricsData => {
            const dataPoints: [number, number][] = metricsData.metrics.map(dataPoint => {
                return [dataPoint.epochSeconds * 1000, dataPoint.value * 1];
            })
            this.chartSeries = [{

                data: dataPoints
            }]
            // this.xAxisStart = Math.min(...chartData.map(v => v[0]))
            // this.yAxisStart = Math.min(...chartData.map(v => v[1]))

            this.changeDetector.markForCheck();
        })
        // this.chartData$ = this.metrics$.pipe(
        //     map(metricsData => {
        //         const chartData = metricsData.metrics.map(dataPoint => {
        //             return [dataPoint.epochSeconds, dataPoint.value * 1] as TuiPoint
        //         })
        //         // this.chartData = chartData;
        //         return chartData;
        //     })
        // )

    }


}

type ChartDataPoint = [number,number]
