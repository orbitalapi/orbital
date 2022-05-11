import {Component, EventEmitter, Inject, Injector, Input, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {QualifiedName, Schema, SchemaMember} from '../../services/schema';
import {map} from 'rxjs/operators';
import {bootstrap} from 'angular';
import {PipelineDirection, PipelineTransportSpec} from '../pipelines.service';
import {BaseTransportConfigEditor} from './base-transport-config-editor';
import {ConnectorSummary} from "../../db-connection-editor/db-importer.service";
import {PolymorpheusComponent} from "@tinkoff/ng-polymorpheus";
import {
  ConnectionEditorContext,
  DbConnectionEditorDialogComponent
} from "../../db-connection-editor/db-connection-editor-dialog.component";
import {TuiDialogService} from "@taiga-ui/core";

@Component({
  selector: 'app-kafka-topic-config',
  template: `
    <div [formGroup]="config">
      <app-form-row title="Kafka connection" helpText="Select the Kafka connection to use">
        <tui-combo-box
          [stringify]="stringifyConnection"
          formControlName="connection">
          Connection name
          <tui-data-list *tuiDataList>
            <button
              tuiOption
              class="link"
              (click)="createNewConnection()"
            >
              <tui-svg src="tuiIconPlusCircleLarge" class="icon"></tui-svg>
              Add new connection...
            </button>
            <button *ngFor="let connection of connections | messageBrokers" tuiOption
                    [value]="connection">{{ connection.connectionName }}</button>
          </tui-data-list>
        </tui-combo-box>
      </app-form-row>
      <app-form-row title="Kafka topic"
                    helpText="Specify the Kafka topic to use">
        <mat-form-field appearance="outline">
          <mat-label>Topic</mat-label>
          <input matInput formControlName="topic" required>
        </mat-form-field>
      </app-form-row>
      <!--      <app-form-row title="Group Id"-->
      <!--                    helpText="The group id defines a set of consumers who will share reading the messages from this topic"-->
      <!--                    *ngIf="direction === 'INPUT'"-->
      <!--      >-->
      <!--        <mat-form-field appearance="outline">-->
      <!--          <mat-label>Group Id</mat-label>-->
      <!--          <input matInput formControlName="groupId">-->
      <!--        </mat-form-field>-->
      <!--      </app-form-row>-->
      <app-form-row title="Payload type"
                    helpText="Set the taxi type that defines the payload that will be provided">
        <app-schema-member-autocomplete
          appearance="outline"
          label="Payload type"
          [schema]="schema"
          [enabled]="editable"
          [selectedMemberName]="targetTypeName"
          (selectedMemberChange)="onTypeSelected($event)"
          schemaMemberType="TYPE"></app-schema-member-autocomplete>
      </app-form-row>
    </div>
  `,
  styleUrls: ['./kafka-topic-config.component.scss']
})
export class KafkaTopicConfigComponent extends BaseTransportConfigEditor {

  config: FormGroup;

  @Output()
  configValueChanged = new EventEmitter<any>();

  readonly stringifyConnection = (item: ConnectorSummary) => item.connectionName;

  // commented out as part of upgrade of Angular / TS.  Was this needed? It's on the base type
  // @Input()
  // schema: Schema;


  @Input()
  direction: PipelineDirection;

  targetTypeName: QualifiedName;

  constructor(@Inject(Injector) private readonly injector: Injector,
              @Inject(TuiDialogService) private readonly dialogService: TuiDialogService) {
    super();
    this.config = new FormGroup({
        topic: new FormControl('', Validators.required),
        targetTypeName: new FormControl('', Validators.required),
        connection: new FormControl('', Validators.required)
      }
    );
    this.config.valueChanges
      .pipe(map(e => {
        const pipelineConfig = {...e}

        // I couldn't work out how to do this inline, so we're using a connection,
        // and then map the connectionName on the way out.
        pipelineConfig.connectionName = (pipelineConfig.connection as ConnectorSummary).connectionName
        return pipelineConfig;
      }))
      .subscribe(e => this.configValueChanged.emit(e));
  }


  onTypeSelected($event: SchemaMember) {
    if ($event) {
      this.config.get('targetTypeName').setValue($event.name.fullyQualifiedName);
      this.payloadTypeChanged.emit($event.name);
    }
  }

  updateFormValues(value: PipelineTransportSpec) {
    this.config.patchValue({
      ...value,
      // bootstrapServer: value.props['bootstrap.servers'],
      // groupId: value.props['group.id']
    });
    this.targetTypeName = QualifiedName.from(value.targetTypeName);
  }


  createNewConnection() {
    this.dialogService.open<ConnectorSummary>(new PolymorpheusComponent(DbConnectionEditorDialogComponent, this.injector),
      {
        data: new ConnectionEditorContext('KAFKA'),
        size: 'l'
      })
      .subscribe((result: ConnectorSummary) => {
        this.connections.push(result);
        this.config.get('connection').setValue(result)
      })
  }

  afterEnabledUpdated(value: boolean) {
    value ? this.config.enable() : this.config.disable();
  }

  // onConnectionSelected($event: string) {
  //   this.config.get('connectionName').setValue($event)
  // }
}
