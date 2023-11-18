import {ChangeDetectionStrategy, ChangeDetectorRef, Component} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {TypesService} from "../services/types.service";
import {Observable} from "rxjs";
import {map, mergeMap} from "rxjs/operators";
import {SavedQuery} from "../services/type-editor.service";
import {MetricsService, StreamMetricsData} from "../services/metrics.service";
import {TuiPoint} from "@taiga-ui/core";
import {TUI_DEFAULT_STRINGIFY, TuiContextWithImplicit} from '@taiga-ui/cdk';

@Component({
    selector: 'app-endpoint-monitor',
    template: `
        <app-header-component-layout *ngIf="query$ | async as query" [title]="query?.name.name">

            <div>
                <div class="">
                    <app-panel-header title="Metrics"></app-panel-header>
                    <tui-axes
                            class="axes"
                            [tuiLineChartHint]="hintContent"
                            [horizontalLines]="2"
                            [verticalLines]="4"
                    >
                        <tui-line-chart
                                [dots]="true"
                                [height]="200"
                                [value]="chartData"
                                [width]="400"
                                [x]="xAxisStart"
                                [y]="yAxisStart"
                                [xStringify]="stringify"
                                [yStringify]="stringify"
                        ></tui-line-chart>
                    </tui-axes>
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
    chartData$: Observable<TuiPoint[]>
    chartData: TuiPoint[] = [
        [50, 50],
        [100, 75],
        [150, 50],
        [200, 150],
        [250, 155],
        [300, 190],
        [350, 90]
    ];
    xAxisStart = 0;
    yAxisStart = 0;
    readonly stringify = TUI_DEFAULT_STRINGIFY;

    readonly hintContent = ({
                                $implicit,
                            }: TuiContextWithImplicit<readonly TuiPoint[]>): number => $implicit[0][1];

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
            const chartData = metricsData.metrics.map(dataPoint => {
                return [dataPoint.epochSeconds, dataPoint.value * 1] as TuiPoint
            })
            this.chartData = chartData;
            this.xAxisStart = Math.min(...chartData.map(v => v[0]))
            // this.yAxisStart = Math.min(...chartData.map(v => v[1]))

            this.changeDetector.markForCheck();
        })
        this.chartData$ = this.metrics$.pipe(
            map(metricsData => {
                const chartData = metricsData.metrics.map(dataPoint => {
                    return [dataPoint.epochSeconds, dataPoint.value * 1] as TuiPoint
                })
                // this.chartData = chartData;
                return chartData;
            })
        )

    }


}
