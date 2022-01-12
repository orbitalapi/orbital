import {Component, OnInit} from '@angular/core';
import {TypesService} from '../services/types.service';
import {ActivatedRoute, ParamMap} from '@angular/router';
import {flatMap, map} from 'rxjs/operators';
import {findType, InstanceLike, Operation, Schema, Service, Type, TypedInstance} from '../services/schema';
import {Fact, QueryService} from '../services/query.service';
import {toOperationSummary} from '../service-view/service-view.component';
import {HttpErrorResponse} from '@angular/common/http';
import { Observable, Subject, ReplaySubject } from 'rxjs';

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
                        [instances$]="operationResult$"
                        [operationResultType]="operationResultType"
                        [operationError]="operationError"
                        [loading]="loading"
                        [schema]="schema"
                        (cancel)="doCancel()"
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
  operationError: HttpErrorResponse;
  loading = false;
  operationResult$: Subject<InstanceLike>;
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
    this.operationResultType = null;
    this.operationError = null;
    this.operationResult$ = new ReplaySubject(5000);
    this.queryService.invokeOperation(summary.serviceName, this.operation.name, parameters)
      .subscribe(result => {
        this.loading = false;
        const retType = findType(this.schema, this.operation.returnTypeName.parameterizedName);
        this.operationResultType = retType.collectionType == null ? retType : retType.collectionType;
        if (result instanceof Array) {
          (result as InstanceLike[]).forEach(data => this.operationResult$.next(data));
        } else {
          this.operationResult$.next(result);
        }
        console.log(result);
      }, (error: HttpErrorResponse) => {
        this.loading = false;
        this.operationError = error;
      });
  }

  doCancel() {
    this.operationResultType = null;
    this.operationError = null;
    this.loading = false;
  }
}
