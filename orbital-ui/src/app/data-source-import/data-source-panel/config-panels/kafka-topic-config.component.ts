import { Component, EventEmitter, Inject, Injector, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TuiButtonModule, TuiDataListModule, TuiDialogService, TuiSvgModule } from '@taiga-ui/core';
import { TuiDataListWrapperModule, TuiInputModule, TuiSelectModule } from '@taiga-ui/kit';
import { PolymorpheusComponent } from '@tinkoff/ng-polymorpheus';
import { ConnectorSummary } from '../../../db-connection-editor/db-importer.service';
import { TypeAutocompleteTuiModule } from '../../../type-autocomplete-tui/type-autocomplete-tui.module';
import { ConvertSchemaEvent, KafkaOffset, KafkaTopicConverterOptions } from '../../data-source-import.models';
import { Schema, Type } from '../../../services/schema';
import {
  ConnectionEditorContext,
  DbConnectionEditorDialogComponent
} from '../../../db-connection-editor/db-connection-editor-dialog.component';
import { PackageIdentifier } from '../../../package-viewer/packages.service';
import { UiCustomisations } from '../../../../environments/ui-customisations';
import { ConnectionFiltersModule } from '../../../utils/connections.pipe';
import { isNullOrUndefined, sanitiseNamespace } from '../../../utils/utils';

@Component({
  selector: 'app-kafka-topic-config',
  standalone: true,
  imports: [
    CommonModule,
    TuiSelectModule,
    FormsModule,
    TuiDataListWrapperModule,
    TuiButtonModule,
    TuiInputModule,
    TuiDataListModule,
    TuiSvgModule,
    ConnectionFiltersModule,
    TypeAutocompleteTuiModule,
  ],
  template: `
    <div class="form-container">
      <form class="form-body" #kafkaForm="ngForm">
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Connection</h3>
            <div class="help-text">
              Select the Kafka connection for the topic you wish to connect to
            </div>
          </div>
          <div class="form-element">
            <tui-select
              [stringify]="stringifyConnection"
              [(ngModel)]="selectedConnection"
              (ngModelChange)="onKafkaConnectionSelected($event)"
              name="connection"
              required
            >
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
                        [value]="connection">{{ connection.connectionName }}
                </button>
              </tui-data-list>
            </tui-select>
          </div>
        </div>
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Topic</h3>
            <div class="help-text">
              Set the topic for {{ uiConfig.productName }} to consume from
            </div>
          </div>
          <div class="form-element">
            <tui-input [(ngModel)]="kafkaTopicOptions.topicName" name="topic" required>
              Kafka topic
            </tui-input>
          </div>
        </div>
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Topic offset</h3>
            <div class="help-text">
              Set the offset rules. This determines where to start reading from when {{ uiConfig.productName }} connects
              to the topic the
              first time.
            </div>
          </div>
          <div class="form-element">
            <tui-select [(ngModel)]="kafkaTopicOptions.offset" name="topicOffset" required>
              Topic offset
              <tui-data-list-wrapper
                *tuiDataList
                [items]="offsets"
              ></tui-data-list-wrapper>
            </tui-select>
          </div>
        </div>
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Namespace</h3>
            <div class="help-text">
              Defines a namespace which the newly exposed Service and Operation will be defined in.
            </div>
          </div>
          <div class="form-element">
            <tui-input [(ngModel)]="kafkaTopicOptions.targetNamespace" name="namespace" required>
              Default namespace
            </tui-input>
          </div>
        </div>
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Message type</h3>
            <div class="help-text">
              Define the message type that is being published on the topic
            </div>
          </div>
          <div class="form-element">
            <app-type-autocomplete-tui
              class="type-input"
              label="Message type"
              size="l"
              [schema]="schema"
              (selectedTypeChanged)="onPayloadTypeSelected($event.member)"
            >
            </app-type-autocomplete-tui>
          </div>
        </div>
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Service and Operation name</h3>
            <div class="help-text">
              Specify a name for the service and operation that will be created for this topic.
              It's ok to leave these blank - a name will be generated for you.
            </div>
          </div>
          <div class="form-element">
            <div tuiGroup class="horizontal-group">
              <div>
                <tui-input
                  [(ngModel)]="kafkaTopicOptions.serviceName"
                  tuiTextfieldExampleText="Service name"
                  class="tui-group__inherit-item"
                  name="serviceName"
                >
                  Service name
                </tui-input>
              </div>
              <div>
                <tui-input
                  [(ngModel)]="kafkaTopicOptions.operationName"
                  tuiTextfieldExampleText="Operation name"
                  class="tui-group__inherit-item"
                  name="operationName"
                >
                  Operation name
                </tui-input>
              </div>
            </div>
          </div>
        </div>
        <input hidden [ngModel]="kafkaTopicOptions.messageType ? 1 : ''" required name="hiddenField"/>
      </form>
    </div>

    <div class="form-button-bar">
      <button tuiButton [showLoader]="working" (click)="doCreate()" [size]="'m'" [disabled]="kafkaForm.invalid">Configure
      </button>
    </div>
  `,
  styleUrls: ['./kafka-topic-config.component.scss']
})
export class KafkaTopicConfigComponent {
  readonly uiConfig = UiCustomisations;
  kafkaTopicOptions: KafkaTopicConverterOptions = new KafkaTopicConverterOptions();

  constructor(@Inject(Injector) private readonly injector: Injector,
              @Inject(TuiDialogService) private readonly dialogService: TuiDialogService) {
  }

  offsets: KafkaOffset[] = ['EARLIEST', 'LATEST'];
  @Input()
  connections: ConnectorSummary[] = [];
  selectedConnection: ConnectorSummary = null;

  @Output()
  loadSchema = new EventEmitter<ConvertSchemaEvent>()

  @Input()
  working: boolean = false;

  @Input()
  schema: Schema;

  @Input()
  packageIdentifier: PackageIdentifier;

  modelType: Type;


  readonly stringifyConnection = (item: ConnectorSummary) => item.connectionName;

  doCreate() {
    console.log(this.kafkaTopicOptions);
    this.loadSchema.next(new ConvertSchemaEvent('kafkaTopic', this.kafkaTopicOptions, this.packageIdentifier));
  }

  createNewConnection() {
    this.dialogService.open<ConnectorSummary>(new PolymorpheusComponent(DbConnectionEditorDialogComponent, this.injector),
      {
        data: new ConnectionEditorContext('KAFKA', null, 'edit', this.packageIdentifier),
        size: 'l'
      })
      .subscribe((result: ConnectorSummary) => {
        this.connections.push(result);
        this.selectedConnection = result;
        this.kafkaTopicOptions.connectionName = result.connectionName
      })
  }

  onPayloadTypeSelected($event: Type) {
    if (isNullOrUndefined($event)) {
      // do nothing
    } else {
      this.kafkaTopicOptions.messageType = $event.name.parameterizedName
    }
  }

  onKafkaConnectionSelected($event: any) {
    const { organisation, name } = this.packageIdentifier;
    this.kafkaTopicOptions.targetNamespace = sanitiseNamespace(`${organisation}.${name}.${$event.connectionName}`);
    this.kafkaTopicOptions.connectionName = $event.connectionName
  }
}
