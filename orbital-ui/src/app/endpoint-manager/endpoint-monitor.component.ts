import {CommonModule, DecimalPipe, TitleCasePipe} from '@angular/common';
import {ChangeDetectionStrategy, ChangeDetectorRef, Component, DestroyRef, Input, OnInit} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormsModule} from '@angular/forms';
import {TuiDataListModule, TuiNotificationModule, TuiTextfieldControllerModule} from '@taiga-ui/core';
import {
  TuiBadgeModule,
  TuiCheckboxLabeledModule,
  TuiProgressModule,
  TuiSelectModule,
  TuiToggleModule
} from '@taiga-ui/kit';
import {
  ApexAxisChartSeries,
  ApexChart,
  ApexDataLabels,
  ApexFill,
  ApexStroke,
  ApexTooltip,
  ApexXAxis,
  ApexYAxis,
  NgApexchartsModule
} from 'ng-apexcharts';
import {BehaviorSubject, combineLatestWith, filter, interval, Observable, of} from 'rxjs';
import {catchError, mergeMap, startWith, tap} from 'rxjs/operators';
import {CodeViewerModule} from '../code-viewer/code-viewer.module';
import {ExpandingPanelSetModule} from '../expanding-panelset/expanding-panel-set.module';
import {HeaderComponentLayoutModule} from '../header-component-layout/header-component-layout.module';
import {DataSeries, MetricsPeriod, MetricsService, StreamMetricsData} from '../services/metrics.service';
import {SavedQuery} from '../services/types.service';
import {UiCustomisations} from "../../environments/ui-customisations";
import {LineageDisplayModule} from "../lineage-display/lineage-display.module";
import {QueryPlan} from "../services/query.service";
import {AppInfoService, FeatureToggles} from "../services/app-info.service";
import {DataSourcesCardComponent} from "../dashboard/data-sources-card/data-sources-card.component";
import {RequiresAuthorityDirective} from "../requires-authority.directive";

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
  tooltip: ApexTooltip;
}

type MetricsPeriodToDescription = {
  period: MetricsPeriod
  label: string,
  autoRefreshEnabled: boolean
}

@Component({
  selector: 'app-endpoint-monitor',
  standalone: true,
  template: `
    <ng-container *ngIf="queryPlan?.steps && featureToggles?.queryPlanModeEnabled">
      <app-panel-header title="Overview" [class.no-header]="onlyShowControlsInHeader"/>
      <app-query-lineage [rows]="queryPlan?.steps"/>
    </ng-container>

    <app-panel-header title="Metrics" [class.no-header]="onlyShowControlsInHeader"
                      *appRequiresAuthority="['ViewMetrics']">
      <ng-content select="header-controls">
      </ng-content>
      <span class="spacer"></span>
      <tui-select class="period-select"
                  tuiTextfieldSize="s"
                  [tuiTextfieldLabelOutside]="true"
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
          <button *ngFor="let period of periods"
                  tuiOption
                  [value]="period">
            {{ period.label }}
          </button>
        </tui-data-list>
      </tui-select>
      <tui-checkbox-labeled size="m" [(ngModel)]="refreshEnabled">Auto refresh</tui-checkbox-labeled>
    </app-panel-header>
    <progress
      max="100"
      tuiProgressBar
      size="xs"
      new
      *ngIf="isChartLoading"
    ></progress>
    <tui-notification status="error" class="error-notification" *ngIf="chartLoadingError">
      {{ chartLoadingError }}
    </tui-notification>
    <tui-notification status="error" class="error-notification" *ngIf="streamLoadingError">
      {{ streamLoadingError }}
    </tui-notification>
    <tui-notification status="neutral" class="error-notification" *ngIf="!metricsAvailable">
      It looks like metrics are unavailable. Check the docs on <a target="_blank" class="link"
                                                                  [href]="UiCustomisations.docsLinks.configureMetricsReporting">how
      to configure Prometheus</a> to capture observability data on queries and streams.
    </tui-notification>

    <div *ngFor="let chartConfig of chartConfigs; trackBy: chartConfigTitle" class="chart-row"
         [class.is-loading]="isChartLoading">
      <div class="label-box">
        <h4 class="label">{{ chartConfig.title }}</h4>
        <div class="hero-datapoint">
          <span>{{ chartConfig.heroDataPoint.value | number: chartConfig.title.toLowerCase().includes('duration') ? '1.0-0' : '1.0-1' }}{{ chartConfig.chartSpec.unitLabel }}</span>
        </div>
      </div>
      <apx-chart [chart]="chartConfig.chartConfig"
                 [series]="chartConfig.series"
                 [stroke]="stroke"
                 [fill]="fill"
                 [yaxis]="chartConfig.yAxis"
                 [dataLabels]="dataLabels"
                 [tooltip]="chartConfig.tooltip"
                 [xaxis]="chartConfig.xAxis"></apx-chart>
    </div>
    <ng-container *appRequiresAuthority="['BrowseSchema']">
      <ng-container *ngIf="(query$ | async) as query">
        <app-panel-header title="Source"></app-panel-header>
        <app-code-viewer [sources]="query.sources"></app-code-viewer>
      </ng-container>
    </ng-container>
  `,
  styleUrls: ['./endpoint-monitor.component.scss'],
  imports: [
    CommonModule,
    HeaderComponentLayoutModule,
    TuiToggleModule,
    TuiBadgeModule,
    ExpandingPanelSetModule,
    TuiSelectModule,
    FormsModule,
    TuiDataListModule,
    TuiCheckboxLabeledModule,
    TuiNotificationModule,
    NgApexchartsModule,
    CodeViewerModule,
    TitleCasePipe,
    DecimalPipe,
    TuiTextfieldControllerModule,
    TuiProgressModule,
    LineageDisplayModule,
    DataSourcesCardComponent,
    RequiresAuthorityDirective
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EndpointMonitorComponent implements OnInit {
  @Input()
  query$: Observable<SavedQuery>

  @Input()
  endpointName$: Observable<string>

  @Input()
  queryPlan: QueryPlan

  @Input()
  streamLoadingError: string

  @Input()
  onlyShowControlsInHeader: boolean;

  @Input()
  isChartLoading: boolean;

  refreshEnabled = true;

  previousPeriodSelected: string;
  previousEndpointSelected: string;

  chartLoadingError: string | null = null;

  readonly periods: MetricsPeriodToDescription[] = [
    {period: 'Last5Minutes', label: 'Last 5 minutes', autoRefreshEnabled: true},
    {period: 'LastHour', label: 'Last hour', autoRefreshEnabled: true},
    {period: 'Last4Hours', label: 'Last 4 hours', autoRefreshEnabled: true},
    {period: 'LastDay', label: 'Last day', autoRefreshEnabled: false},
    {period: 'Last7Days', label: 'Last 7 days', autoRefreshEnabled: false},
    /*{ period: 'Last30Days', label: 'Last 30 days', autoRefreshEnabled: false },*/
  ]
  readonly selectedPeriod$: BehaviorSubject<MetricsPeriodToDescription> = new BehaviorSubject(this.periods[1])
  dataLabels: ApexDataLabels = {
    enabled: false
  }
  stroke: ApexStroke = {
    width: 2,
    curve: 'straight'
  }
  fill: ApexFill = {
    type: 'gradient',
    gradient: {
      shadeIntensity: 1,
      opacityFrom: 0.7,
      opacityTo: 0.9,
      //stops: [0, 90, 100]
    }
  }
  chartConfigs: ChartConfig[] = [];
  metricsAvailable: boolean = true;

  readonly chartConfigTitle = (index: number, item: ChartConfig): string => item.title;
  featureToggles: FeatureToggles;
  readonly stringifyPeriod = (item: MetricsPeriodToDescription) => item.label;

  constructor(
    private metricsService: MetricsService,
    private changeDetector: ChangeDetectorRef,
    private destroyRef: DestroyRef,
    private appInfoService: AppInfoService
  ) {
    appInfoService.getConfig()
      .subscribe(next => {
        this.featureToggles = next.featureToggles
      });
  }

  ngOnInit(): void {
    const ticks$ = interval(15000)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        startWith(0),
        filter(() => this.refreshEnabled)
      )

    this.endpointName$
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        combineLatestWith(this.selectedPeriod$, ticks$),
        tap(([endpoint, {period}]) => {
          if (this.previousPeriodSelected !== period || this.previousEndpointSelected !== endpoint) {
            this.isChartLoading = true;
            this.previousPeriodSelected = period;
            this.previousEndpointSelected = endpoint;
            this.changeDetector.markForCheck();
          }
        }),
        mergeMap(([endpoint, period]) => {
          return this.metricsService.getMetricsForStream(endpoint, period.period).pipe(
            catchError(error => {
              console.log(error);
              this.chartLoadingError = 'There was a problem loading the chart data';
              this.isChartLoading = false;
              this.changeDetector.markForCheck();
              // Return an observable to prevent the stream from completing
              return of(null);
            })
          );
        })
      )
      .subscribe(metricsData => {
        if (metricsData) {
          this.updateChartConfig(metricsData);
        }
        this.chartLoadingError = null;
        this.isChartLoading = false;
        this.changeDetector.markForCheck();
      })
  }

  private updateChartConfig(metricsData: StreamMetricsData) {
    this.metricsAvailable = metricsData.metricsAvailable;
    this.chartConfigs = metricsData.series.map(dataSeries => {
      const dataPoints: [number, number][] = dataSeries.series.map(dataPoint => {
          let value: number = isNumeric(dataPoint.value.toString()) ? dataPoint.value * 1 : 0;
          if (dataSeries.unit === 'DurationInSecondsConvertToMillis') {
            value = value * 1000
          }
          return [dataPoint.epochSeconds * 1000, value]
        }
      )
      let heroDataPoint;
      if (dataPoints.length > 0) {
        heroDataPoint = dataPoints[dataPoints.length - 1][1]
      } else {
        heroDataPoint = ''
      }
      return {
        title: dataSeries.title,
        heroDataPoint: {
          value: heroDataPoint,
          label: dataSeries.unitLabel,
        },
        series: [
          {
            name: dataSeries.title,
            data: dataPoints,
          }
        ],
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
            type: 'x',
            enabled: true,
            autoScaleYaxis: true
          },
          toolbar: {
            autoSelected: 'zoom'
          }
        },
        xAxis: {
          type: 'datetime',
          tooltip: {
            enabled: false
          }
        },
        yAxis: {
          labels: {
            minWidth: 40, // must be set for a group of syncronized charts
            formatter: val => {
              return val.toFixed(dataSeries.title.toLowerCase().includes('duration') ? 0 : 1)
            }
          }
        },
        tooltip: {
          x: {
            format: 'dd/MM/yyyy HH:mm:ss',
          },
          y: {
            formatter: (val: number) => `${val.toFixed(dataSeries.title.toLowerCase().includes('duration') ? 0 : 1)}${dataSeries.unitLabel}`
          }
        }
      }
    });

    this.changeDetector.markForCheck();
  }

  protected readonly UiCustomisations = UiCustomisations;
}

function isNumeric(str: any): boolean {
  if (typeof str != 'string') return false // we only process strings!
  return !isNaN(parseFloat(str))
}
