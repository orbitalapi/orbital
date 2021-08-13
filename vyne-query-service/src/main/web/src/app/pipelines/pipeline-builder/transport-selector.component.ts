import {Component, Input, ViewEncapsulation} from '@angular/core';
import {PipelineTransport} from '../pipelines.service';
import {FormControl, FormGroup} from '@angular/forms';

@Component({
  selector: 'app-transport-selector',
  template: `
    <div class="pipeline-transport-form-field">
      <mat-form-field appearance="outline">
        <mat-label>Pipeline {{direction}}</mat-label>
        <input matInput placeholder="Select a {{direction}}" aria-label="State" [matAutocomplete]="auto"
               [formControl]="control"
               >
        <mat-autocomplete #auto="matAutocomplete" [displayWith]="pipelineTransportLabel">
          <mat-option class="pipeline-transport-option" *ngFor="let pipelineSource of transports"
                      [value]="pipelineSource">
            <div class="header">{{ pipelineSource.label }}</div>
            <div class="description">{{ pipelineSource.description }}</div>
          </mat-option>
        </mat-autocomplete>
      </mat-form-field>
    </div>
  `,
  encapsulation: ViewEncapsulation.None,
  styleUrls: ['./transport-selector.component.scss']
})
export class TransportSelectorComponent {

  @Input()
  direction: 'source' | 'target';
  @Input()
  transports: PipelineTransport[];
  @Input()
  formGroup: FormGroup;
  @Input()
  formControlName: string;
  @Input()
  control: FormControl;

  pipelineTransportLabel(transport: PipelineTransport): string {
    return transport ? transport.label : '';
  }
}
