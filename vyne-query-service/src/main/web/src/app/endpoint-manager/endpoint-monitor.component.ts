import {ChangeDetectionStrategy, ChangeDetectorRef, Component} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {TypesService} from "../services/types.service";
import {
    BehaviorSubject,
    combineLatestAll,
    combineLatestWith,
    filter,
    interval,
    Observable,
    of,
    ReplaySubject
} from "rxjs";
import {combineLatest, map, mergeMap, startWith} from "rxjs/operators";
import {SavedQuery} from "../services/type-editor.service";
import {DataSeries, MetricsPeriod, MetricsService, StreamMetricsData} from "../services/metrics.service";
import {
    ApexAxisChartSeries,
    ApexChart,
    ApexDataLabels,
    ApexFill,
    ApexStroke,
    ApexXAxis,
    ApexYAxis
} from "ng-apexcharts";
import {tuiIsNumber} from "@taiga-ui/cdk";
import {WorkspaceMembershipDto} from "../services/workspaces.service";

@Component({
    selector: 'app-endpoint-monitor',
    template: `
        <app-header-component-layout *ngIf="query$ | async as query" [title]="query?.name.name"
                                     [subtitle]="query.queryKind"
                                     [iconUrl]="getIconUrl(query.queryKind)"
        >
            <ng-container ngProjectAs="header-components">
                <div *ngIf="query.httpEndpoint" class="url-parts">
                    <span class="method">{{query.httpEndpoint.method}}</span>
                    <span class="url">{{query.httpEndpoint.url}}</span>
                </div>
            </ng-container>

            <div>
                <div class="">
                    <app-panel-header title="Metrics">
                        <span class="spacer"></span>

                        <tui-select class="period-select"
                                    tuiTextfieldSize="s"
                                    [stringify]="stringifyPeriod"
                                    [ngModel]="selectedPeriod$ | async"
                                    (ngModelChange)="selectedPeriod$.next($event)"
                        >
                            Period
                            <input
                                    placeholder="Period"
                                    tuiTextfield
                            />
                            <tui-data-list *tuiDataList>
                                <button class="small" *ngFor="let period of periods" tuiOption
                                        [value]="period">{{period.label}}</button>
                            </tui-data-list>
                        </tui-select>
                        <tui-checkbox-labeled size="m" [(ngModel)]="refreshEnabled">Auto refresh</tui-checkbox-labeled>
                    </app-panel-header>
                    <div *ngFor="let chartConfig of chartConfigs" class="chart-row">
                        <div class="label-box">
                            <h4 class="label">{{chartConfig.title}}</h4>
                            <div class="hero-datapoint">
                                <span>{{chartConfig.heroDataPoint.value | number: '1.0-1'}}{{chartConfig.chartSpec.unitLabel}}</span>
                            </div>
                        </div>
                        <apx-chart [chart]="chartConfig.chartConfig" [series]="chartConfig.series"
                                   [stroke]="stroke"
                                   [fill]="fill"
                                   [yaxis]="chartConfig.yAxis"
                                   [dataLabels]="dataLabels"
                                   [xaxis]="chartConfig.xAxis"></apx-chart>

                    </div>
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
    refreshEnabled = true;

    readonly periods: MetricsPeriodToDescription[] = [
        {period: "Last5Minutes", label: 'Last 5 minutes', autoRefreshEnabled: true},
        {period: "LastHour", label: 'Last hour', autoRefreshEnabled: true},
        {period: "Last4Hours", label: 'Last 4 hours', autoRefreshEnabled: true},
        {period: "LastDay", label: 'Last day', autoRefreshEnabled: false},
        {period: "Last7Days", label: 'Last 7 days', autoRefreshEnabled: false},
        {period: "Last30Days", label: 'Last 30 days', autoRefreshEnabled: false},
    ]
    readonly stringifyPeriod = (item: MetricsPeriodToDescription) => item.label;

    selectedPeriod$: BehaviorSubject<MetricsPeriodToDescription> = new BehaviorSubject(this.periods[1])

    getIconUrl(queryKind: 'Stream' | 'Query') {
        switch (queryKind) {
            case "Query": return "assets/img/tabler/arrows-right-left.svg";
            case "Stream": return "assets/img/tabler/arrows-right.svg";

        }
    }

    dataLabels: ApexDataLabels = {
        enabled: false
    }

    stroke: ApexStroke = {
        width: 2,
        curve: "straight"
    }

    fill: ApexFill = {
        type: "gradient",
        gradient: {
            shadeIntensity: 1,
            opacityFrom: 0.7,
            opacityTo: 0.9,
            stops: [0, 90, 100]
        }
    }
    chartConfigs: ChartConfig[] = [];


    constructor(
        private activatedRoute: ActivatedRoute,
        private typeService: TypesService,
        private metricsService: MetricsService,
        private changeDetector: ChangeDetectorRef
    ) {

        const ticks$ = interval(15000)
            .pipe(
                startWith(0),
                filter(() => this.refreshEnabled))

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

        endpointName$.pipe(
            combineLatestWith(this.selectedPeriod$, ticks$),
            mergeMap(value => {
                const [endpoint, period] = value;
                return metricsService.getMetricsForStream(endpoint, period.period)
            })
        ).subscribe(metricsData => {
            this.chartConfigs = metricsData.series.map(dataSeries => {
                const dataPoints: [number, number][] = dataSeries.series.map(dataPoint => {
                        let value: number = isNumeric(dataPoint.value.toString()) ? dataPoint.value * 1 : 0;
                        if (dataSeries.unit === "DurationInSecondsConvertToMillis") {
                            value = value * 1000
                        }
                        return [dataPoint.epochSeconds * 1000, value]
                    }
                )
                let heroDataPoint;
                if (dataPoints.length > 0) {
                    heroDataPoint = dataPoints[dataPoints.length - 1][1]
                } else {
                    heroDataPoint = ""
                }
                return {
                    title: dataSeries.title,
                    heroDataPoint: {
                        value: heroDataPoint,
                        label: dataSeries.unitLabel
                    },
                    series: [{
                        name: dataSeries.title,
                        data: dataPoints,
                    }],
                    chartSpec: dataSeries,
                    chartConfig: {
                        type: 'area',
                        animations: {
                            enabled: false
                        },
                        height: 150,
                        id: `chart-${dataSeries.title}`,
                        group: 'metrics',

                        zoom: {
                            type: "x",
                            enabled: true,
                            autoScaleYaxis: true
                        },
                        toolbar: {
                            autoSelected: "zoom"
                        }
                    },
                    xAxis: {
                        type: "datetime"
                    },
                    yAxis: {
                        labels: {
                            minWidth: 40, // must be set for a group of syncronized charts
                            formatter: val => {
                                return val.toFixed(0)
                            }
                        }
                    }
                }
            });

            this.changeDetector.markForCheck();
        })
    }


}

function isNumeric(str: any): boolean {
    if (typeof str != "string") return false // we only process strings!
    return !isNaN(parseFloat(str))
}

type ChartConfig = {
    title: string;
    heroDataPoint: {
        value: any;
        label: string;
    }
    chartSpec: DataSeries;
    chartConfig: ApexChart;
    series: ApexAxisChartSeries
    yAxis: ApexYAxis;
    xAxis: ApexXAxis;
}

type MetricsPeriodToDescription = {
    period: MetricsPeriod
    label: string,
    autoRefreshEnabled: boolean
}
