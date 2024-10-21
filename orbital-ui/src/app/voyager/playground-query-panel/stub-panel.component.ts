import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Inject,
  Injector,
  Input,
  Output
} from '@angular/core';
import {CommonModule} from '@angular/common';
import {OperationStub} from "../../services/query.service";
import {TuiTableModule} from "@taiga-ui/addon-table";
import {TuiButtonModule, TuiDataListModule, TuiDialogService} from "@taiga-ui/core";
import {TuiComboBoxModule} from "@taiga-ui/kit";
import {collectionAllOperations, Operation, Schema, ServiceMember} from "../../services/schema";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {PolymorpheusComponent} from "@tinkoff/ng-polymorpheus";
import {ResponseEditorDialogComponent} from "./response-editor-dialog.component";
import {StubDesignerComponent, StubDesignerProps} from "../stub-designer/stub-designer.component";
import {isNullOrUndefined} from "../../utils/utils";

@Component({
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ResponseEditorDialogComponent,
    TuiTableModule,
    TuiButtonModule,
    TuiComboBoxModule,
    TuiDataListModule],
  selector: 'app-stub-panel',
  standalone: true,
  styleUrls: ['./stub-panel.component.scss'],
  template: `
    <div>Define stubs to simulate responses from operations such as API calls, Kafka topics, or databases</div>
    <table tuiTable [columns]="columns">
      <thead>
      <tr tuiThGroup>
        <th tuiTh [resizable]="true">
          Operation
        </th>
        <th tuiTh class="no-rhs-border">Response</th>
        <th tuiTh></th>
      </tr>
      </thead>
      <tbody tuiTbody [data]="stubs">
      <tr tuiTr *ngFor="let stub of stubs">

        <td *tuiCell="'operationName'" tuiTd>
          <tui-combo-box
            [(ngModel)]="stub.operationName"
          >Operation
            <tui-data-list *tuiDataList>
              <button *ngFor="let operation of operations" tuiOption
                      [value]="operation.qualifiedName.shortDisplayName">{{ operation.qualifiedName.longDisplayName }}
              </button>
            </tui-data-list>
          </tui-combo-box>
        </td>
        <td *tuiCell="'response'" [class.disabled]="isNullOrUndefined(stub.operationName)" tuiTd class="response-cell no-rhs-border" (click)="showResponseEditorDialog(stub)">
          {{ stub.response }}
        </td>
        <td tuiTd *tuiCell="'button'" class="button-cell">
          <button tuiButton appearance="icon" icon="tuiIconTrash" size="xs" (click)="removeStub(stub)"></button>
        </td>
      </tr>
      </tbody>
    </table>
    <div>
      <button tuiButton size="s" appearance="outline" (click)="addNewStub()">Add new</button>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StubPanelComponent {

  constructor(@Inject(TuiDialogService) private readonly dialogs: TuiDialogService,
              @Inject(Injector) private readonly injector: Injector,
              private readonly changeDetector: ChangeDetectorRef) {
  }

  @Input()
  stubs: OperationStub[] = [];

  @Output()
  stubsChange = new EventEmitter<OperationStub[]>();

  @Input()
  schema: Schema;

  get operations():ServiceMember[] {
    if (!this.schema) {
      return [];
    }
    return collectionAllOperations(this.schema)
  }

  readonly columns = ['operationName', 'response', 'button']

  showResponseEditorDialog(stub: OperationStub) {
    if (isNullOrUndefined(stub.operationName)) {
      return
    }

    const operation = this.operations.find(op => op.name == stub.operationName)
    this.dialogs.open<OperationStub>(
      new PolymorpheusComponent(StubDesignerComponent, this.injector),
      {
        size: "auto",
        closeable: true,
        appearance: 'no-border',
        data: {
          operation,
          stub
        } as StubDesignerProps,
        dismissible: true,
      },
    ).subscribe(next => {
      this.stubs[this.stubs.indexOf(stub)] = next;
      this.changeDetector.markForCheck();
    });

  }

  addNewStub() {
    this.stubs.push({
      operationName: null,
      response: ''
    })
    this.stubsChange.emit(this.stubs);
  }

  removeStub(stub: OperationStub) {
    this.stubs.splice(this.stubs.indexOf(stub), 1)
    this.stubsChange.emit(this.stubs);
  }

  protected readonly isNullOrUndefined = isNullOrUndefined;
}
