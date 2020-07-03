import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { CaskConfigRecord } from 'src/app/services/cask.service';

@Component({
  selector: 'app-cask-row',
  templateUrl: './cask-row.component.html',
  styleUrls: ['./cask-row.component.scss']
})
export class CaskRowComponent implements OnInit {

  constructor() { }

  @Output()
  onCaskConfigClick = new EventEmitter<CaskConfigRecord>();

  @Input()
  typeName: string;

  @Input()
  caskConfigs: CaskConfigRecord[];

  ngOnInit() {
  }

}
