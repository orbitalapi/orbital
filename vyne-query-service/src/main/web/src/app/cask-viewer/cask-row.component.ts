import {Component, OnInit, Input, Output, EventEmitter} from '@angular/core';
import {CaskConfigRecord} from 'src/app/services/cask.service';

@Component({
  selector: 'app-cask-row',
  styleUrls: ['./cask-row.component.scss'],
  template: `
    <div class="cask-summary-row" (click)="onCaskConfigClick.emit(caskConfigs[0])">
      <div class="cask-name">{{ typeName }}</div>
    </div>
  `
})
export class CaskRowComponent  {

  constructor() {
  }

  @Output()
  onCaskConfigClick = new EventEmitter<CaskConfigRecord>();

  @Input()
  typeName: string;

  @Input()
  caskConfigs: CaskConfigRecord[];

}
