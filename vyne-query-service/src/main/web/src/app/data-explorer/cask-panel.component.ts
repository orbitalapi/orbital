import {Component, EventEmitter, Input, Output} from '@angular/core';
import { HttpParameterCodec } from '@angular/common/http';
import {CsvOptions, XmlIngestionParameters} from '../services/types.service';
import {CaskService} from '../services/cask.service';

@Component({
  selector: 'app-cask-panel',
  template: `
    <mat-progress-bar *ngIf="loading" mode="query" color="accent"></mat-progress-bar>
    <mat-expansion-panel>
      <mat-expansion-panel-header>
        <mat-panel-title>
          <img class="icon" src="assets/img/cask.svg">
          Store this data in a cask
        </mat-panel-title>
      </mat-expansion-panel-header>

      <p>Casks are a lightweight storage of raw data, that can be queried semantically, and automatically publish their
        data to Vyne. Store this data in a cask now by
        sending to this url:</p>
      <div class="url-section">
        <div class="http-method">POST</div>
        <div class="http-url">{{ url }}</div>
      </div>

      <div class="button-row">
        <button mat-stroked-button (click)="send()" [disabled]="loading">Send</button>
        <span *ngIf="resultMessage">{{ resultMessage }}</span>
      </div>
    </mat-expansion-panel>
  `,
  styleUrls: ['./cask-panel.component.scss']
})
export class CaskPanelComponent {

  constructor(private caskService: CaskService) {
  }

  @Input()
  csvOptions: CsvOptions;

  @Input()
  contents: string;

  @Input()
  targetTypeName: string;

  @Input()
  format: string;

  @Input()
  caskServiceUrl: string;

  @Input()
  loading = false;

  @Input()
  xmlIngestionParameters: XmlIngestionParameters;

  resultMessage: string;

  get url() {
    let caskUrl = `${this.caskServiceUrl}/api/ingest/${this.format}/${this.targetTypeName}`;
    if (this.format === 'csv') {
      const csvOptions = this.csvOptions;
      const nullValueArg = (this.csvOptions.nullValueTag) ? `&nullValue=${encodeURIComponent(csvOptions.nullValueTag)}` : '';
      const ignoreContentBeforeArg = (this.csvOptions.ignoreContentBefore) ? `&ignoreContentBefore=${encodeURIComponent(csvOptions.ignoreContentBefore)}` : '';
      const csvOptionsQueryString = `?delimiter=${encodeURIComponent(csvOptions.separator)}&firstRecordAsHeader=${csvOptions.firstRecordAsHeader}` +
        `&containsTrailingDelimiters=${csvOptions.containsTrailingDelimiters}${nullValueArg}${ignoreContentBeforeArg}`;
      caskUrl += csvOptionsQueryString;
    } else if (this.format === 'xml') {
      const elementSelector = this.xmlIngestionParameters.elementSelector;
      const elementSelectorArg = elementSelector ? `?elementSelector=${encodeURIComponent(elementSelector)}` : '';
      caskUrl += elementSelectorArg;
    }
    return caskUrl;
  }

  send() {
    this.loading = true;
    this.caskService.publishToCask(this.url, this.contents)
      .subscribe(result => {
        this.loading = false;
        this.resultMessage = result.result + (result.result === 'REJECTED' ? `: ${result.message}` : '');
      }, error => {
        this.loading = false;
        this.resultMessage = error.error ? `Error: ${error.error.message}` : error.message;
      });
  }
}