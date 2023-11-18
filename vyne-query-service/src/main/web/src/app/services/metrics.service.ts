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

    getMetricsForStream(streamName: string):Observable<StreamMetricsData> {
        return this.http.get<StreamMetricsData>(`${this.environment.serverUrl}/api/metrics/stream/${streamName}`)
    }
}

export interface PipelineTags {
    application: string;
    instance: string;
    job: string;
    pipeline: string;
}
export interface StreamMetricsData {
    tags: PipelineTags;
    metrics: MetricTimestampValue[]
}

export interface MetricTimestampValue {
    timestamp: Date;
    epochSeconds: number;
    value: any;
}
