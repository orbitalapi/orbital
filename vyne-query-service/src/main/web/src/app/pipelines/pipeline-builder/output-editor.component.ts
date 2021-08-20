/* tslint:disable:max-line-length */
import {Component, OnInit} from '@angular/core';
import {BaseTransportEditorComponent} from './base-transport-editor';

@Component({
  selector: 'app-output-editor',
  template: `
    <div class="pipeline-transport-config-panel"
         *ngIf="pipelineTransportSpecFg"
         [formGroup]="pipelineTransportSpecFg">
      <h4>{{ label }}</h4>
      <div [ngSwitch]="transportSpecType">
        <app-kafka-topic-config *ngSwitchCase="'kafka'" [schema]="schema"
                                (configValueChanged)="updateConfigValue($event)"
                                [editable]="editable"
                                [pipelineTransportSpec]="pipelineTransportSpec"
                                direction="OUTPUT"
        ></app-kafka-topic-config>
        <app-cask-output-config *ngSwitchCase="'cask'" [schema]="schema"
                                [editable]="editable"
                                [pipelineTransportSpec]="pipelineTransportSpec"
                                (configValueChanged)="updateConfigValue($event)"></app-cask-output-config>
        <app-operation-output-config *ngSwitchCase="'taxiOperation'" [schema]="schema"
                                     [editable]="editable"
                                     [pipelineTransportSpec]="pipelineTransportSpec"
                                     (configValueChanged)="updateConfigValue($event)"></app-operation-output-config>
      </div>
    </div>
  `,
  styleUrls: ['./pipeline-builder.component.scss']
})
export class OutputEditorComponent extends BaseTransportEditorComponent {


}
