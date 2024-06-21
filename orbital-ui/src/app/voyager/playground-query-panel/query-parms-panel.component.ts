import {Component, EventEmitter, Input, Output} from '@angular/core';
import { CommonModule } from '@angular/common';
import {TuiButtonModule, TuiDataListModule} from "@taiga-ui/core";
import {TuiComboBoxModule, TuiInputModule} from "@taiga-ui/kit";
import {TuiTableModule} from "@taiga-ui/addon-table";
import {FormsModule} from "@angular/forms";

type KeyValuePair = {name: string, value: any}
@Component({
  selector: 'app-query-params-panel',
  standalone: true,
  imports: [CommonModule, TuiButtonModule, TuiComboBoxModule, TuiDataListModule, TuiTableModule, FormsModule, TuiInputModule],
  template: `
    <div>Parameters let you pass values to a query at runtime</div>
    <div *ngIf="!paramList || paramList.length === 0">
      You have no parameters defined in your query.
    </div>
    <table tuiTable [columns]="columns" *ngIf="paramList?.length > 0">
      <thead>
      <tr tuiThGroup>
        <th tuiTh [resizable]="true">
          Parameter
        </th>
        <th tuiTh>Value</th>
      </tr>
      </thead>
      <tbody tuiTbody>
      <tr tuiTr *ngFor="let parameter of paramList">

        <td *tuiCell="'parameterName'" tuiTd>
          {{ parameter.name }}
        </td>
        <td *tuiCell="'value'" tuiTd class="">
          <tui-input [ngModel]="parameter.value" (ngModelChange)="updateParam(parameter, $event)">
            <input tuiTextfield/>
          </tui-input>
        </td>
      </tr>
      </tbody>
    </table>
  `,
  styleUrls: ['./query-parms-panel.component.scss']
})
export class QueryParmsPanelComponent {
  private _parameters:  { [index: string]: any };
  paramList:KeyValuePair[];
  @Input()
  get parameters(): { [p: string]: any } {
    return this._parameters;
  }

  set parameters(value: { [p: string]: any }) {
    this._parameters = value;
    if (this.parameters) {
      this.paramList = Object.keys(this.parameters).map(key => {
        return { name: key, value: this.parameters[key]}
      })
    }
  }
  readonly columns = ['parameterName', 'value']



  @Output()
  parametersChange = new EventEmitter<{ [index: string]: any }>()

  updateParam(parameter: KeyValuePair, value: string) {
    parameter.value = value;
    this.parameters[parameter.name] = value;
    this.parametersChange.emit(this._parameters);
  }
}
