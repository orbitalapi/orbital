import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { CaskConfigRecord } from '../services/cask.service';
import { VersionedSource } from '../services/schema';

@Component({
  selector: 'app-cask-details',
  templateUrl: './cask-details.component.html',
  styleUrls: ['./cask-details.component.scss']
})
export class CaskDetailsComponent implements OnInit {
  private _caskConfig: CaskConfigRecord;
  sources: VersionedSource[];

  constructor() { }

  @Output()
  deleteCask = new EventEmitter<CaskConfigRecord>();

  ngOnInit() {
  }



  @Input()
  get caskConfig(): CaskConfigRecord {
    return this._caskConfig;
  }

  set caskConfig(value: CaskConfigRecord) {
    this._caskConfig = value;
    this.sources = this.computeSources();
  }

  computeSources(): VersionedSource[] {
    return this.caskConfig.sources.map (
      (source, index) => ({name: this.caskConfig.sourceSchemaIds[index],  version: '1', content: source }) );
  }
}
