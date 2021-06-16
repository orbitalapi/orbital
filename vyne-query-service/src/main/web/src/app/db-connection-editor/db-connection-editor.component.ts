import {Component, EventEmitter, Input, Output} from '@angular/core';
import {DbConnectionService, JdbcConnectionConfiguration, JdbcDriverConfigOptions} from './db-importer.service';
import {ComponentType, DynamicFormComponentSpec, InputType} from './dynamic-form-component.component';
import {FormControl, FormGroup, Validators} from '@angular/forms';

@Component({
  selector: 'app-db-connection-editor',
  templateUrl: './db-connection-editor.component.html',
  styleUrls: ['./db-connection-editor.component.scss']
})
export class DbConnectionEditorComponent {
  selectedDriver: JdbcDriverConfigOptions;
  formElements: DynamicFormComponentSpec[];

  working = false;
  testResult: ConnectionTestResult;

  get hasTestFailure() {
    return this.testResult && this.testResult.success === false;
  }

  get testSuccessful() {
    return this.testResult && this.testResult.success;
  }

  @Input()
  drivers: JdbcDriverConfigOptions[];

  connectionDetails: FormGroup;
  driverParameters: FormGroup; // A nested formGroup within the driverParameters

  constructor(private dbConnectionService: DbConnectionService) {
    this.buildDefaultFormGroupControls();
  }

  private buildDefaultFormGroupControls() {
    const currentValue = (this.connectionDetails) ?
      this.connectionDetails.getRawValue() :
      {};
    this.connectionDetails = new FormGroup({
      connectionName: new FormControl(currentValue.connectionName || '', Validators.required),
      driver: new FormControl(currentValue.driver || '', Validators.required)
    });
  }

  setDriver(driverName: string) {
    this.selectedDriver = this.drivers.find(s => s.driverName === driverName);
    this.buildFormInputs();
  }

  private buildFormInputs() {
    const elements: DynamicFormComponentSpec[] = this.selectedDriver.parameters.map(param => {
      let componentType: ComponentType = 'input';
      let inputType: InputType = 'text';
      if (param.dataType === 'STRING' && param.sensitive) {
        inputType = 'password';
      } else if (param.dataType === 'NUMBER') {
        inputType = 'number';
      } else if (param.dataType === 'BOOLEAN') {
        componentType = 'checkbox';
        inputType = null;
      }
      const formElement = new DynamicFormComponentSpec(
        componentType,
        param.templateParamName,
        param.displayName,
        param.required,
        inputType,
        param.defaultValue,
      );
      return formElement;
    });
    const formGroup = this.connectionDetails.controls;
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
    this.dbConnectionService.createConnection(this.connectionDetails.getRawValue())
      .subscribe(testResult => {
        this.working = false;
        this.testResult = {
          success: true
        };
      }, error => {
        this.working = false;
        this.testResult = {
          success: false,
          errorMessage: error.error.message
        };
      });
  }

  doTestConnection() {
    this.working = true;
    this.dbConnectionService.testConnection(this.connectionDetails.getRawValue())
      .subscribe(testResult => {
        this.working = false;
        this.testResult = {
          success: true
        };
      }, error => {
        this.working = false;
        this.testResult = {
          success: false,
          errorMessage: error.error.message
        };
      });
  }
}

export interface ConnectionTestResult {
  success: boolean;
  errorMessage?: string | null;
}
