import {Component, Input, OnInit} from '@angular/core';
import {DataSource, TypeNamedInstance} from '../services/schema';

@Component({
  selector: 'app-lineage-timeline',
  templateUrl: './lineage-timeline.component.html',
  styleUrls: ['./lineage-timeline.component.scss']
})
export class LineageTimelineComponent {


  private _dataSource: DataSource;
  private _instance: TypeNamedInstance;

  orientation = 'TD';

  @Input()
  get dataSource(): DataSource {
    return this._dataSource;
  }

  set dataSource(value: DataSource) {
    if (this._dataSource === value) {
      return;
    }
    this._dataSource = value;
    this.buildTimeline();
  }

  @Input()
  get instance(): TypeNamedInstance {
    return this._instance;
  }

  set instance(value: TypeNamedInstance) {
    if (this._instance === value) {
      return;
    }
    this._instance = value;
    this.buildTimeline();
  }

  buildTimeline() {
    if (!this.instance || !this.dataSource) {
      return;
    }
    debugger;
  }

}
