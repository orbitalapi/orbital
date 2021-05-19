import {Component, OnInit, Input, Output, EventEmitter} from '@angular/core';
import {CaskConfigRecord} from '../services/cask.service';
import {VersionedSource} from '../services/schema';
import {QualityReport} from '../quality-cards/quality.service';

@Component({
  selector: 'app-cask-details',
  templateUrl: './cask-details.component.html',
  styleUrls: ['./cask-details.component.scss']
})
export class CaskDetailsComponent {
  private _caskConfig: CaskConfigRecord;
  sources: VersionedSource[];

  caskDisplayName: string;
  @Output()
  deleteCask = new EventEmitter<CaskConfigRecord>();

  @Input()
  qualityReport: QualityReport;

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
