/* eslint-disable max-len */
import { Component } from '@angular/core';
import { BaseTransportEditorComponent } from './base-transport-editor';

@Component({
  selector: 'app-input-editor',
  template: `
    <div class="pipeline-transport-config-panel"
         *ngIf="pipelineTransportSpecFg"
         [formGroup]="pipelineTransportSpecFg">
      <h4>{{label}}</h4>
      <div [ngSwitch]="transportSpecType">
        <app-polling-operation-input-config *ngSwitchCase="'taxiOperation'" [schema]="schema"
                                            (configValueChanged)="updateConfigValue($event)"
                                            [pipelineTransportSpec]="pipelineTransportSpec"
                                            [editable]="editable"
                                            (payloadTypeChanged)="payloadTypeChanged.emit($event)"
        ></app-polling-operation-input-config>
        <app-polling-query-input-config *ngSwitchCase="'query'" [schema]="schema"
                                        (configValueChanged)="updateConfigValue($event)"
                                        [pipelineTransportSpec]="pipelineTransportSpec"
                                        [editable]="editable"
                                        (payloadTypeChanged)="payloadTypeChanged.emit($event)"
        ></app-polling-query-input-config>
        <app-http-listener-input-config *ngSwitchCase="'httpListener'" [schema]="schema"
                                        [pipelineTransportSpec]="pipelineTransportSpec"
                                        [editable]="editable"
                                        (payloadTypeChanged)="payloadTypeChanged.emit($event)"
                                        (configValueChanged)="updateConfigValue($event)"></app-http-listener-input-config>
        <app-kafka-topic-config *ngSwitchCase="'kafka'" [schema]="schema"
                                direction="INPUT"
                                [pipelineTransportSpec]="pipelineTransportSpec"
                                [editable]="editable"
                                [connections]="connections"
                                (payloadTypeChanged)="payloadTypeChanged.emit($event)"
                                (configValueChanged)="updateConfigValue($event)"></app-kafka-topic-config>
        <app-sqs-s3-listener-input-config *ngSwitchCase="'awsSnsS3'"
                                          [schema]="schema"
                                          [connections]="connections"
                                          direction="INPUT"
                                          [pipelineTransportSpec]="pipelineTransportSpec"
                                          [editable]="editable"
                                          (payloadTypeChanged)="payloadTypeChanged.emit($event)"
                                          (configValueChanged)="updateConfigValue($event)">
        </app-sqs-s3-listener-input-config>
      </div>
    </div>`,
  styleUrls: ['./pipeline-builder.component.scss']
})
export class InputEditorComponent extends BaseTransportEditorComponent {


}
