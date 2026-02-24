import { TuiComboBoxModule, TuiInputModule } from "@taiga-ui/legacy";
import { CommonModule } from '@angular/common';
import {Component, EventEmitter, Input, Output} from '@angular/core';
import { TuiError, TuiButton } from '@taiga-ui/core';
import { TuiDataListWrapper, TuiStringifyContentPipe, TuiFilterByInputPipe, TuiFieldErrorPipe, TuiProgress } from '@taiga-ui/kit';
import { HeaderComponentLayoutComponent } from '../header-component-layout/header-component-layout.component';
import { ProjectSelectorModule } from '../project-selector/project-selector.module';
import {
  ConnectionDriverConfigOptions,
  ConnectionStatus,
  ConnectorSummary,
  ConnectorType,
  DbConnectionService,
  JdbcConnectionConfiguration
} from './db-importer.service';
import { DynamicFormComponentComponent, DynamicFormComponentSpec, InputType } from './dynamic-form-component.component';
import {
  FormsModule,
  ReactiveFormsModule,
  UntypedFormControl,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import {isNullOrUndefined} from '../utils/utils';
import {SourcePackageDescription} from "../package-viewer/packages.service";
import {Observable} from "rxjs";

export type ConnectionEditorMode = 'create' | 'edit';

@Component({
  selector: 'app-connection-editor',
  templateUrl: './connection-editor.component.html',
  styleUrls: ['./connection-editor.component.scss'],
  imports: [
    CommonModule,
    HeaderComponentLayoutComponent,
    ProjectSelectorModule,
    ReactiveFormsModule,
    TuiInputModule,
    TuiError,
    TuiComboBoxModule,
    TuiDataListWrapper,
    TuiFieldErrorPipe,
    TuiFilterByInputPipe,
    DynamicFormComponentComponent,
    TuiButton,
    TuiProgress,
    TuiStringifyContentPipe,
    FormsModule
  ],
  standalone: true
})
export class ConnectionEditorComponent {
  selectedDriver: ConnectionDriverConfigOptions;
  formElements: DynamicFormComponentSpec[];

  @Input()
  packages$: Observable<SourcePackageDescription[]>;

  @Input()
  selectedPackage: SourcePackageDescription = null;

  @Input()
  selectedDriverId: string | null = null;

  @Input()
  filterConnectorTypes: ConnectorType | null = null;

  @Input()
  mode: ConnectionEditorMode = 'create';

  working = false;
  testResult: ConnectionStatus;


  get hasTestFailure() {
    return this.testResult && this.testResult.status === 'ERROR';
  }

  get testSuccessful() {
    return this.testResult && this.testResult.status === 'OK';
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

  connectionDetails: UntypedFormGroup;
  driverParameters: UntypedFormGroup; // A nested formGroup within the connectionDetails

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
    this.connectionDetails = new UntypedFormGroup({
      connectionName: new UntypedFormControl(currentValue.connectionName || '', Validators.required),
      jdbcDriver: new UntypedFormControl(currentValue.driver || '', Validators.required)
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
        let inputType: InputType = 'text';
        if (param.dataType === 'STRING' && param.sensitive) {
          inputType = 'password';
        } else if (param.dataType === 'NUMBER') {
          inputType = 'number';
        } else if (param.dataType === 'BOOLEAN') {
          inputType = "checkbox"
        }
        return new DynamicFormComponentSpec(
          param.templateParamName,
          param.displayName,
          param.description,
          param.required,
          inputType,
          param.isConstructorParameter,
          param.defaultValue
        );
      });
    const connectionParameters = {};
    elements.forEach(element => {
      connectionParameters[element.key] = element.required ?
        new UntypedFormControl(element.value, Validators.required) :
        new UntypedFormControl(element.value);
    });
    this.driverParameters = new UntypedFormGroup(connectionParameters);
    this.connectionDetails.setControl('connectionParameters', this.driverParameters);
    this.formElements = elements;
  }


  get isValid() {
    return this.connectionDetails.valid && this.selectedPackage !== null;
  }

  createConnection() {
    this.working = true;
    this.dbConnectionService.createConnection(this.selectedPackage.identifier, this.getConnectionConfiguration())
      .subscribe(result => {
        this.working = false;
        this.testResult = result.connectionStatus
        this.connectionCreated.emit(result);
      }, error => {
        this.working = false;
        this.testResult = {
          status: 'ERROR',
          message: error?.error?.message || 'An unknown error occurred',
          timestamp: new Date()
        };
      });
  }

  private getConnectionConfiguration(): JdbcConnectionConfiguration {
    const connectionParameters = this.connectionDetails.getRawValue().connectionParameters || {};
    const constructorParameters = this.selectedDriver.parameters
      .filter(param => param.isConstructorParameter)
      .reduce((previousValue, currentValue) => {
        if (!isNullOrUndefined(connectionParameters[currentValue.templateParamName])) {
          return {[currentValue.templateParamName]: connectionParameters[currentValue.templateParamName], ...previousValue}
        } else {
          return previousValue
        }
      }, {})
    // ConnectionParams must be sent to the server as a Map<String,String>,
    // or deserialization errors occur
    // See: ORB-132
    Object.keys(connectionParameters).map(key => {
      if (constructorParameters[key]) {
        delete connectionParameters[key]
      } else if (isNullOrUndefined(connectionParameters[key])) {
        delete connectionParameters[key]
      } else {
        connectionParameters[key] = String(connectionParameters[key]);
      }
    });
    return {
      ...this.connectionDetails.getRawValue(),
      connectionParameters,
      ...constructorParameters,
      jdbcDriver: this.selectedDriver.driverName,
      type: this.selectedDriver.connectorType
    }
  }

  doTestConnection() {
    this.working = true;
    this.dbConnectionService.testConnection(this.selectedPackage.identifier, this.getConnectionConfiguration())
      .subscribe(testResult => {
        this.working = false;
        this.testResult = testResult;
      }, error => {
        this.working = false;
        this.testResult = {
          status: 'ERROR',
          message: error?.error?.message || 'An unknown error occurred',
          timestamp: new Date()
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

