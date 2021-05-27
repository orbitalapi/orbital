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

export enum DataQualitySubject {
  Operation = 'Operation',
  DataStore = 'DataStore',
  Message = 'Message'
}

export interface AveragedScoreBySubject {
  subjectTypeName: string;
  subjectKind: DataQualitySubject;
  score: number;
  recordCount: number;
}

export interface QualityReport {
  overallScore: number;
  numberOfEvents: number;
  overallGrade: RuleGrade;
  averagedScoreByDate: AveragedScoreByDate[];
  ruleSummaries: QualityRuleSummary[];
  averagedScoreBySubject: AveragedScoreBySubject[];
}

export interface QualityRuleSummary {
  ruleName: string;
  grade: RuleGrade;
  recordCount: number;
  score: number;
}

export interface RuleGradedEvaluation {
  ruleName: string;
  evaluations: AverageAttributeEvaluationForPath[];
}

export interface AverageAttributeEvaluationForPath {
  path: string;
  recordCount: number;
  grade: RuleGrade;
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

  loadRuleDetailReport(typeName: string, ruleName: string, period: Period): Observable<RuleGradedEvaluation> {
    return this.http.get<RuleGradedEvaluation>(`${environment.qualityHubUrl}/api/events/${typeName}/rule/${ruleName}/period/${period}`);
  }
}

