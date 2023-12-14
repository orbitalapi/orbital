import { Component, Input } from '@angular/core';
import { QualifiedName, Schema, Service } from '../services/schema';
import { TypesService } from '../services/types.service';
import { getCatalogType } from 'src/app/operation-view/operation-view.component';
import { OperationSummary, toOperationSummary } from 'src/app/service-view/operation-summary';
import { methodClassFromName } from 'src/app/service-view/service-view-class-utils';


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
          <app-schema-diagram [schema]="schema" [displayedMembers]="[service.name.parameterizedName]"></app-schema-diagram>
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
                <td><span class="mono-badge"><a [routerLink]="['/catalog',navigationTargetForType(operation.returnType)]">{{ operation.returnType.shortDisplayName }}</a></span></td>
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

  schema: Schema;
  constructor(typeService:TypesService) {
    typeService.getTypes().subscribe(s => this.schema = s)
  }

  navigationTargetForType(name: QualifiedName): string {
    // Can't call directly, because function is not accessible via angular template
    return getCatalogType(name);
  }


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


