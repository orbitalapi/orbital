import {Inject, Injectable, Injector} from "@angular/core";
import {Environment, ENVIRONMENT} from "./environment";
import {TuiDialogService} from "@taiga-ui/core";
import {HttpClient} from "@angular/common/http";
import {SchemaNotificationService} from "./schema-notification.service";
import {Observable} from "rxjs";


@Injectable({
    providedIn: 'root',
})
export class MetricsService {
    constructor(
        @Inject(ENVIRONMENT) private environment: Environment,
        private http: HttpClient,
    ) {}

    getMetricsForStream(streamName: string, period: MetricsPeriod):Observable<StreamMetricsData> {
        return this.http.get<StreamMetricsData>(`${this.environment.serverUrl}/api/metrics/stream/${streamName}?period=${period}`)
    }
}

export interface StreamMetricsData {
    tags: { [index: string]: any };
    series: DataSeries[]
}
export interface DataSeries {
  title: string;
  unitLabel: string;
  unit: 'Count' | 'DurationInSeconds' | 'DurationInSecondsConvertToMillis';
  series: MetricTimestampValue[]
}

export interface MetricTimestampValue {
    timestamp: Date;
    epochSeconds: number;
    value: any;
}

export type MetricsPeriod = 'Last5Minutes' | 'LastHour' | 'Last4Hours' | 'LastDay' | 'Last7Days' | 'Last30Days';
