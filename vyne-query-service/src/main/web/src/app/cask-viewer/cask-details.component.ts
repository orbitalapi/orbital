import {Component, OnInit, Input, Output, EventEmitter} from '@angular/core';
import {CaskConfigRecord} from '../services/cask.service';
import {VersionedSource} from '../services/schema';
import {QualityReport} from '../quality-cards/quality.service';

@Component({
  selector: 'app-cask-details',
  template: `
    <div class="page-content">
      <h2>{{caskDisplayName}}</h2>

      <div class="results-details">
        <app-key-value title="Type" [value]="caskConfig.qualifiedTypeName"></app-key-value>
        <app-key-value title="Created at" [value]="caskConfig.insertedAt | date: 'medium'"></app-key-value>
        <app-key-value title="Number of records" [value]="caskConfig.details.recordsNumber"></app-key-value>
        <app-key-value title="Number of errors"
                       [value]="caskConfig.details.ingestionErrorsInLast24Hours"></app-key-value>
        <div class="spacer"></div>
        <button
          (click)="deleteCask.emit(caskConfig)"
          [disabled]="caskConfig.exposesType === true"
          mat-icon-button
          [matTooltip]="caskConfig.exposesType === false ? 'Delete this cask' : 'A View based Cask can not be deleted!'">
          <i class="material-icons">delete</i>
        </button>
      </div>

      <app-quality-card-container [qualitySubjectTypeName]="caskConfig?.qualifiedTypeName"></app-quality-card-container>
      <app-cask-ingestion-errors [caskConfigRecord]="caskConfig"></app-cask-ingestion-errors>
    </div>
  `,
  styleUrls: ['./cask-details.component.scss']
})
export class CaskDetailsComponent {
  private _caskConfig: CaskConfigRecord;
  sources: VersionedSource[];

  caskDisplayName: string;
  @Output()
  deleteCask = new EventEmitter<CaskConfigRecord>();

  @Input()
  get caskConfig(): CaskConfigRecord {
    return this._caskConfig;
  }

  set caskConfig(value: CaskConfigRecord) {
    if (this.caskConfig === value) {
      return;
    }
    this._caskConfig = value;
    this.sources = this.computeSources();
    this.caskDisplayName = this.caskConfig.qualifiedTypeName.split('.').pop();

  }

  computeSources(): VersionedSource[] {
    return this.caskConfig.sources.map(
      (source, index) => ({name: this.caskConfig.sourceSchemaIds[index], version: '1', content: source}));
  }
}
