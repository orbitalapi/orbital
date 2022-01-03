import {Component, OnInit} from '@angular/core';
import {TypesService} from '../services/types.service';
import {ActivatedRoute, ParamMap} from '@angular/router';
import {flatMap, map} from 'rxjs/operators';
import {findType, Operation, Schema, Service, Type, TypedInstance} from '../services/schema';
import {Fact, QueryService} from '../services/query.service';
import {toOperationSummary} from '../service-view/service-view.component';
import {HttpErrorResponse} from '@angular/common/http';

@Component({
  selector: 'app-operation-view-container',
  template: `
    <mat-toolbar color="primary">
      <span>Operation Explorer</span>
      <span class="toolbar-spacer"></span>
      <app-search-bar-container></app-search-bar-container>
    </mat-toolbar>
    <app-operation-view [operation]="operation"
                        (submit)="invokeOperation($event)"
                        [operationResult]="operationResult"
                        [operationResultType]="operationResultType"
                        [operationError]="operationError"
                        [loading]="loading"
                        [schema]="schema"
                        class="page-content"
    ></app-operation-view>
  `,
  styleUrls: ['./operation-view-container.component.scss']
})
export class OperationViewContainerComponent implements OnInit {

  constructor(private typeService: TypesService,
              private activeRoute: ActivatedRoute,
              private queryService: QueryService) {
  }

  schema: Schema;
  operation: Operation;
  operationResult: TypedInstance;
  operationError: HttpErrorResponse;
  loading = false;

  operationResultType: Type;

  ngOnInit() {
    this.typeService.getTypes()
      .subscribe(schema => this.schema = schema);
    this.activeRoute.paramMap.pipe(
      map((params: ParamMap) => {
        return {
          serviceName: params.get('serviceName'),
          operationName: params.get('operationName')
        };
      }),
      flatMap(params => {
        return this.typeService.getOperation(params.serviceName, params.operationName);
      })
    ).subscribe((operation: Operation) => {
      this.operation = operation;
    });
  }

  invokeOperation(parameters: { [index: string]: Fact }) {
    const summary = toOperationSummary(this.operation);
    this.loading = true;
    this.operationResult = null;
    this.operationResultType = null;
    this.operationError = null;
    this.queryService.invokeOperation(summary.serviceName, this.operation.name, parameters)
      .subscribe(result => {
        this.loading = false;
        this.operationResult = result;
        this.operationResultType = findType(this.schema, this.operation.returnTypeName.parameterizedName);
        console.log(result);
      }, (error: HttpErrorResponse) => {
        this.loading = false;
        this.operationError = error;
      });
  }
}
