import {Component, EventEmitter, Input, Output} from '@angular/core';
import {
  ConnectorSummary,
  DbConnectionService,
  JdbcConnectionConfiguration,
  ConnectionDriverConfigOptions, ConnectorType
} from './db-importer.service';
import {ComponentType, DynamicFormComponentSpec} from './dynamic-form-component.component';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {TuiInputModeT, TuiInputTypeT} from '@taiga-ui/cdk';
import {isNullOrUndefined} from 'util';

export type ConnectionEditorMode = 'create' | 'edit';

@Component({
  selector: 'app-connection-editor',
  templateUrl: './connection-editor.component.html',
  styleUrls: ['./connection-editor.component.scss']
})
export class ConnectionEditorComponent {
  selectedDriver: ConnectionDriverConfigOptions;
  formElements: DynamicFormComponentSpec[];

  @Input()
  selectedDriverId: string | null = null;

  @Input()
  filterConnectorTypes: ConnectorType | null = null;

  @Input()
  mode: ConnectionEditorMode = 'create';

  working = false;
  testResult: ConnectionTestResult;


  get hasTestFailure() {
    return this.testResult && this.testResult.success === false;
  }

  get testSuccessful() {
    return this.testResult && this.testResult.success;
  }

  @Input()
  drivers: ConnectionDriverConfigOptions[] = [];

  private _connector: ConnectorSummary
  @Input()
  get connector(): ConnectorSummary {
    return this._connector;
  }

  set connector(value) {
    if (this._connector === value) {
      return;
    }
    this._connector = value;
    if (!isNullOrUndefined(this.connector) && !isNullOrUndefined(this.drivers)) {
      this.rebuildForm();
    }

  }

  @Output()
  connectionCreated = new EventEmitter<ConnectorSummary>();

  connectionDetails: FormGroup;
  driverParameters: FormGroup; // A nested formGroup within the driverParameters

  constructor(private dbConnectionService: DbConnectionService) {
    dbConnectionService.getDrivers()
      .subscribe(drivers => {
        // Being a little lazy here.  If we update this to not call the server all the time,
        // then we also need to cater for applying the filters (filterConnectorTypes & SelectedDriverId)
        // after the results have been returned
        this.drivers = drivers;
        if (!isNullOrUndefined(this.filterConnectorTypes)) {
          this.drivers = this.drivers.filter(driver => driver.connectorType === this.filterConnectorTypes);
        }
        if (this.connector) {
          this.rebuildForm();
        }
        if (this.selectedDriverId) {
          this.selectedDriver = this.drivers.find(driver => driver.driverName === this.selectedDriverId);
        }
      });
    this.buildDefaultFormGroupControls();
  }

  private buildDefaultFormGroupControls() {
    const currentValue = (this.connectionDetails) ?
      this.connectionDetails.getRawValue() :
      {};
    this.connectionDetails = new FormGroup({
      connectionName: new FormControl(currentValue.connectionName || '', Validators.required),
      jdbcDriver: new FormControl(currentValue.driver || '', Validators.required)
    });
  }

  setDriver() {
    if (isNullOrUndefined(this.selectedDriver)) {
      return;
    }
    // this.selectedDriver = this.drivers.find(s => s.driverName === driverName);
    this.buildFormInputs();
  }

  readonly stringifyJdbcDriver = (item: ConnectionDriverConfigOptions) => item.displayName;

  private buildFormInputs() {
    const elements: DynamicFormComponentSpec[] = this.selectedDriver.parameters
        .filter(param => param.visible)
        .map(param => {
      let componentType: ComponentType = 'input';
      let textFieldType: TuiInputTypeT = 'text';
      let textFieldMode: TuiInputModeT = 'text';
      if (param.dataType === 'STRING' && param.sensitive) {
        textFieldType = 'password';
      } else if (param.dataType === 'NUMBER') {
        textFieldMode = 'numeric';
      } else if (param.dataType === 'BOOLEAN') {
        componentType = 'checkbox';
        textFieldType = null;
      }
      return new DynamicFormComponentSpec(
        componentType,
        param.templateParamName,
        param.displayName,
        param.required,
        textFieldMode,
        textFieldType,
        param.defaultValue,
      );
    });
    const connectionParameters = {};
    elements.forEach(element => {
      connectionParameters[element.key] = element.required ?
        new FormControl(element.value, Validators.required) :
        new FormControl(element.value);
    });
    this.driverParameters = new FormGroup(connectionParameters);
    this.connectionDetails.setControl('connectionParameters', this.driverParameters);
    this.formElements = elements;
  }


  get isValid() {
    return this.connectionDetails.valid;
  }

  createConnection() {
    this.working = true;
    this.dbConnectionService.createConnection(this.getConnectionConfiguration())
      .subscribe(result => {
        this.working = false;
        this.testResult = {
          success: true,
          message: 'Connection created.'
        };
        this.connectionCreated.emit(result);
      }, error => {
        this.working = false;
        this.testResult = {
          success: false,
          message: error.error.message
        };
      });
  }

  private getConnectionConfiguration(): JdbcConnectionConfiguration {
    return {
      ...this.connectionDetails.getRawValue(),
      jdbcDriver: this.selectedDriver.driverName,
      connectionType: this.selectedDriver.connectorType
    }
  }

  doTestConnection() {
    this.working = true;
    this.dbConnectionService.testConnection(this.getConnectionConfiguration())
      .subscribe(testResult => {
        this.working = false;
        this.testResult = {
          success: true,
          message: 'Connection tested successfully'
        };
      }, error => {
        this.working = false;
        this.testResult = {
          success: false,
          message: error.error.message
        };
      });
  }

  private rebuildForm() {
    // This is where I'm up to.
    // Editing has turned out to be annoying, so I'm stopping for now.
    // We only recieve a subset of values in the UI (although we send everything)
    // so edits need a new mechanism, which also need to consider sensitve vales.
    // Given this isn't a prioriy rihgt now, I'm just removing the Edit feature.

    if (isNullOrUndefined(this.drivers) || isNullOrUndefined(this.connector)) {
      return;
    }
    this.selectedDriver = this.drivers.find(driver => this.connector.driverName === driver.driverName);
    this.buildFormInputs();
  }
}

export interface ConnectionTestResult {
  success: boolean;
  message?: string | null;
}
