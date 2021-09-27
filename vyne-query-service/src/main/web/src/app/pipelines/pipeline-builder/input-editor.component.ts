/* tslint:disable:max-line-length */
import {Component, Input, OnInit} from '@angular/core';
import {Schema} from '../../services/schema';
import {AbstractControl, FormGroup} from '@angular/forms';
import {PipelineTransport} from '../pipelines.service';
import {BaseTransportEditorComponent} from './base-transport-editor';

@Component({
  selector: 'app-input-editor',
  template: `
    <div class="pipeline-transport-config-panel"
         *ngIf="pipelineTransportSpecFg"
         [formGroup]="pipelineTransportSpecFg">
      <h4>{{label}}</h4>
      <div [ngSwitch]="transportSpecType">
        <app-polling-input-config *ngSwitchCase="'taxiOperation'" [schema]="schema"
                                  (configValueChanged)="updateConfigValue($event)"
                                  [pipelineTransportSpec]="pipelineTransportSpec"
                                  [editable]="editable"
                                  (payloadTypeChanged)="payloadTypeChanged.emit($event)"
        ></app-polling-input-config>
        <app-http-listener-input-config *ngSwitchCase="'httpListener'" [schema]="schema"
                                        [pipelineTransportSpec]="pipelineTransportSpec"
                                        [editable]="editable"
                                        (payloadTypeChanged)="payloadTypeChanged.emit($event)"
                                        (configValueChanged)="updateConfigValue($event)"></app-http-listener-input-config>
        <app-kafka-topic-config *ngSwitchCase="'kafka'" [schema]="schema"
                                direction="INPUT"
                                [pipelineTransportSpec]="pipelineTransportSpec"
                                [editable]="editable"
                                (payloadTypeChanged)="payloadTypeChanged.emit($event)"
                                (configValueChanged)="updateConfigValue($event)"></app-kafka-topic-config>
      </div>
    </div>`,
  styleUrls: ['./pipeline-builder.component.scss']
})
export class InputEditorComponent extends BaseTransportEditorComponent {


}
