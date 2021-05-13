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
