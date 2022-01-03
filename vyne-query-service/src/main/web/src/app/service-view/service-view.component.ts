import {Component, Input} from '@angular/core';
import {fqn, Operation, QualifiedName, Service} from '../services/schema';
import {isNullOrUndefined} from 'util';

export interface OperationSummary {
  name: string;
  typeDoc: string | null;
  url: string;
  method: string;
  returnType: QualifiedName;
  serviceName: string;
}

export interface OperationName {
  serviceName: string;
  serviceDisplayName: string;
  operationName: string;
}

export function splitOperationQualifiedName(name: string): OperationName {
  const nameParts = name.split('@@');

  return {
    serviceName: nameParts[0],
    serviceDisplayName: fqn(nameParts[0]).shortDisplayName,
    operationName: nameParts[1]
  };
}

export function toOperationSummary(operation: Operation): OperationSummary {
  const httpOperationMetadata = operation.metadata.find(metadata => metadata.name.fullyQualifiedName === 'HttpOperation');
  const method = httpOperationMetadata ? httpOperationMetadata.params['method'] : null;
  const url = httpOperationMetadata ? httpOperationMetadata.params['url'] : null;

  const nameParts = splitOperationQualifiedName(operation.qualifiedName.fullyQualifiedName);
  const serviceName = nameParts.serviceName;
  return {
    name: operation.name,
    method: method,
    url: url,
    typeDoc: operation.typeDoc,
    returnType: operation.returnTypeName,
    serviceName
  } as OperationSummary;
}

@Component({
  selector: 'app-service-view',
  template: `
    <div class="page-content">
      <div class="documentation" *ngIf="service">
        <div class="page-heading">
          <h1>{{service?.name?.name}}</h1>
          <span class="mono-badge">{{service?.name?.fullyQualifiedName}}</span>
        </div>

        <section>
          <app-description-editor-container [type]="service"></app-description-editor-container>
        </section>

        <section *ngIf="service">
          <h2>Operations</h2>
          <div>
            <table class="operation-list">
              <thead>
              <tr>
                <th>Method</th>
                <th>Name</th>
                <th>Description</th>
                <th>Return type</th>
                <th>Url</th>
              </tr>
              </thead>
              <tr *ngFor="let operation of operationSummaries">
                <td [ngClass]="getMethodClass(operation.method)">
                  <span class="http-method" [ngClass]="getMethodClass(operation.method)">{{ operation.method }}</span>
                </td>
                <td><a [routerLink]="[operation.name]" data-e2e-id="operation-name">{{ operation.name }}</a></td>
                <td>{{ operation.typeDoc }}</td>
                <td><span class="mono-badge">{{ operation.returnType.shortDisplayName }}</span></td>
                <td><span class="url">{{ operation.url }}</span></td>
              </tr>
            </table>
          </div>
        </section>

        <section>
          <h2>Lineage</h2>
          <p class="help-text">This chart shows how this service depends on others.</p>

          <app-service-lineage-graph-container [serviceName]="service?.name"></app-service-lineage-graph-container>
        </section>
      </div>
    </div>

  `,
  styleUrls: ['./service-view.component.scss']
})
export class ServiceViewComponent {

  private _service: Service;

  operationSummaries: OperationSummary[];

  @Input()
  get service(): Service {
    return this._service;
  }

  set service(value: Service) {
    if (this._service === value) {
      return;
    }
    this._service = value;
    this.buildOperationSummary();
  }

  private buildOperationSummary() {
    this.operationSummaries = this.service.operations.map(operation => toOperationSummary(operation));
  }

  getMethodClass(method: string) {
    return methodClassFromName(method);
  }
}


export function methodClassFromName(method: string) {
  if (isNullOrUndefined(method)) {
    return null;
  }
  switch (method.toUpperCase()) {
    case 'GET':
      return 'get-method';
    case 'POST':
      return 'post-method';
    case 'PUT':
      return 'put-method';
    case 'DELETE':
      return 'delete-method';
    default:
      return 'other-method';
  }
}
