import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { CaskConfigRecord } from '../services/cask.service';
import { MatDialog } from '@angular/material';
import { CaskConfirmDialogComponent } from './cask-confirm-dialog.component';
import { VersionedSource } from '../services/schema';

@Component({
  selector: 'app-cask-details',
  templateUrl: './cask-details.component.html',
  styleUrls: ['./cask-details.component.scss']
})
export class CaskDetailsComponent implements OnInit {

  constructor() { }

  @Output()
  onDeleteCask = new EventEmitter<CaskConfigRecord>();

  @Output()
  onClearCask = new EventEmitter<CaskConfigRecord>();

  ngOnInit() {
  }

  private _caskConfig: CaskConfigRecord;
  sources: VersionedSource[];

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
