import {Component, Input} from '@angular/core';
import {QualifiedName, Service} from '../services/schema';

interface OperationSummary {
  name: string;
  typeDoc: string | null;
  url: string;
  method: string;
  returnType: QualifiedName;
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
                <td>{{ operation.name }}</td>
                <td>{{ operation.typeDoc }}</td>
                <td><span class="mono-badge">{{ operation.returnType.shortDisplayName }}</span></td>
                <td><span class="url">{{ operation.url }}</span></td>
              </tr>
            </table>
          </div>
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
    this.operationSummaries = this.service.operations.map(operation => {
      const httpOperationMetadata = operation.metadata.find(metadata => metadata.name.fullyQualifiedName === 'HttpOperation');
      const method = httpOperationMetadata.params['method'];
      const url = httpOperationMetadata.params['url'];

      return {
        name: operation.name,
        method: method,
        url: url,
        typeDoc: operation.typeDoc,
        returnType: operation.returnType
      } as OperationSummary;
    });
  }

  getMethodClass(method: string) {
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
}
