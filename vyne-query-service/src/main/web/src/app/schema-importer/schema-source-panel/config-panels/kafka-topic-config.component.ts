import {Component, EventEmitter, Inject, Injector, Input, OnInit, Output} from '@angular/core';
import {ConnectorSummary} from '../../../db-connection-editor/db-importer.service';
import {ConvertSchemaEvent, KafkaOffset, KafkaTopicConverterOptions} from '../../schema-importer.models';
import {Schema, Type} from '../../../services/schema';
import {PolymorpheusComponent} from '@tinkoff/ng-polymorpheus';
import {
  ConnectionEditorContext,
  DbConnectionEditorDialogComponent
} from '../../../db-connection-editor/db-connection-editor-dialog.component';
import {TuiDialogService} from '@taiga-ui/core';
import {isNullOrUndefined} from 'util';

@Component({
  selector: 'app-kafka-topic-config',
  template: `
    <div class="form-container">
      <div class="form-body">
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Connection</h3>
            <div class="help-text">
              Select the Kafka connection for the topic you wish to connect to
            </div>
          </div>
          <div class="form-element">
            <tui-combo-box
              [stringify]="stringifyConnection"
              [(ngModel)]="selectedConnection"
              (ngModelChange)="kafkaTopicOptions.connectionName = $event.connectionName">
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
          </div>
        </div>
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Topic</h3>
            <div class="help-text">
              Set the topic for Vyne to consume from
            </div>
          </div>
          <div class="form-element">
            <tui-input [(ngModel)]="kafkaTopicOptions.topicName">
              Kafka topic
            </tui-input>
          </div>
        </div>
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Topic offset</h3>
            <div class="help-text">
              Set the offset rules. This determines where to start reading from when Vyne connects to the topic the
              first time.
            </div>
          </div>
          <div class="form-element">
            <tui-select [(ngModel)]="kafkaTopicOptions.offset">
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
            <tui-input [(ngModel)]="kafkaTopicOptions.targetNamespace">
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
            <app-type-autocomplete [schema]="schema"
                                   label="Message type"
                                   [(selectedType)]="modelType"
                                   (selectedTypeChange)="onPayloadTypeSelected($event)"
            ></app-type-autocomplete>

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
            <div tuiGroup class="group">
              <div>
                <tui-input
                  [(ngModel)]="kafkaTopicOptions.serviceName"
                  tuiTextfieldExampleText="Service name"
                  class="tui-group__inherit-item"
                >
                  Service name
                </tui-input>
              </div>
              <div>
                <tui-input
                  [(ngModel)]="kafkaTopicOptions.operationName"
                  tuiTextfieldExampleText="Operation name"
                  class="tui-group__inherit-item"
                >
                  Operation name
                </tui-input>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="form-button-bar">
      <button tuiButton [showLoader]="working" (click)="doCreate()" [size]="'m'">Create
      </button>
    </div>
  `,
  styleUrls: ['./kafka-topic-config.component.scss']
})
export class KafkaTopicConfigComponent {

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

  modelType: Type;


  readonly stringifyConnection = (item: ConnectorSummary) => item.connectionName;

  doCreate() {
    console.log(this.kafkaTopicOptions);
    this.loadSchema.next(new ConvertSchemaEvent('kafkaTopic', this.kafkaTopicOptions));
  }

  createNewConnection() {
    this.dialogService.open<ConnectorSummary>(new PolymorpheusComponent(DbConnectionEditorDialogComponent, this.injector),
      {
        data: new ConnectionEditorContext('KAFKA'),
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
}
