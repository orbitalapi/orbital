import {Injectable} from '@angular/core';
import {VyneServicesModule} from '../services/vyne-services.module';
import {HttpClient} from '@angular/common/http';
import {environment} from 'src/environments/environment';
import {Observable} from 'rxjs/index';

export enum RuleGrade {
  GOOD = 'GOOD',
  WARNING = 'WARNING',
  BAD = 'BAD'
}

export interface AveragedScoreByDate {
  date: Date;
  score: number;
  recordCount: number;
}

export interface QualityReport {
  overallScore: number;
  overallGrade: RuleGrade;
  averageByDate: AveragedScoreByDate[];
  ruleSummaries: QualityRuleSummary[];
}

export interface QualityRuleSummary {
  title: string;
  grade: RuleGrade;
  count: number;
  score: number;
}

export enum Period {
  Today = 'Today',
  Yesterday = 'Yesterday',
  Last7Days = 'Last7Days',
  Last30Days = 'Last30Days'
}

@Injectable({
  providedIn: VyneServicesModule
})
export class DataQualityService {
  constructor(private http: HttpClient) {

  }

  loadQualityReport(typeName: string, period: Period): Observable<QualityReport> {
    return this.http.get<QualityReport>(`${environment.qualityHubUrl}/api/events/${typeName}/period/${period}`);
  }
}

