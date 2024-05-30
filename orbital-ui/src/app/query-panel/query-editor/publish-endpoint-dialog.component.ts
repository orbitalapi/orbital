import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {TuiAlertService, TuiDialogContext, TuiNotification} from '@taiga-ui/core';
import {POLYMORPHEUS_CONTEXT} from '@tinkoff/ng-polymorpheus';
import {
  AddOrRemoveHttpEndpointAnnotationEvent,
  AddOrRemoveWebsocketEndpointAnnotationEvent,
  HttpMethod,
  SavedQueryWithSource,
  SchemaEdit,
  SchemaEditOperation,
  SchemaImporterService
} from '../../project-import/schema-importer.service';
import {QueryKind} from '../../services/types.service';
import {SaveQueryRequestProps} from './save-query-dialog.component';

export type EndpointType = 'HTTP' | 'WEBSOCKET'

export interface PublishEndpointPanelProps extends SaveQueryRequestProps {
  endpointType: EndpointType
  queryKind: QueryKind
}

@Component({
  selector: 'app-publish-endpoint-dialog',
  template: `
    <app-header-component-layout
      [title]="title">
      <form [formGroup]="formGroup">
        <div *ngIf="operation === 'Remove'" class="remove-endpoint-text">
          This will remove the {{this.context.data.endpointType | titlecase}} endpoint where this query is currently accessible.<br/>
          Any future requests to <span class="mono-badge">{{pathIfExists}}</span> will result in a 404 error. Cool with that?
        </div>
        <tui-input
          formControlName="endpoint"
          [tuiTextfieldPrefix]="context.data.endpointType === 'HTTP' ? httpPrefix : websocketPrefix"
          [pseudoFocus]="true"
          [class.hidden]="operation === 'Remove'"
          tuiAutoFocus
        >
          API Endpoint
          <input tuiTextfield/>
        </tui-input>
        <tui-select
          *ngIf="context.data.endpointType === 'HTTP' && operation === 'Add'"
          formControlName="httpMethod"
        >
          Select HTTP method
          <tui-data-list *tuiDataList>
            <button *ngFor='let verb of httpMethods' tuiOption [value]='verb'>{{ verb }}</button>
          </tui-data-list>
        </tui-select>
        <tui-error
          formControlName="endpoint"
          [error]="[] | tuiFieldError | async"
        ></tui-error>
      </form>
      <tui-notification *ngIf="errorMessage" [status]="'error'">{{ errorMessage }}</tui-notification>
      <div class="row">
        <button
          tuiButton
          type="button"
          size="m"
          appearance="outline"
          (click)="close()"
        >
          Cancel
        </button>
        <div class="spacer"></div>
        <button
          tuiButton
          type="button"
          size="m"
          [appearance]="operation === 'Add' ? 'primary' : 'accent'"
          [disabled]="!formGroup.valid"
          (click)="update()"
        >
          {{operation}}
        </button>
      </div>
    </app-header-component-layout>
  `,
  styleUrls: ['./publish-endpoint-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PublishEndpointDialogComponent {
  formGroup: FormGroup
  errorMessage: string;
  httpMethods: HttpMethod[];

  readonly httpPrefix = '/api/q/'
  readonly websocketPrefix = '/api/s/'

  constructor(
    @Inject(TuiAlertService) private readonly alerts: TuiAlertService,
    @Inject(POLYMORPHEUS_CONTEXT)
    readonly context: TuiDialogContext<SavedQueryWithSource, PublishEndpointPanelProps>,
    private schemaImporterService: SchemaImporterService,
    private changeDetector: ChangeDetectorRef
  ) {
    this.formGroup = new FormGroup({
      endpoint: new FormControl(null,
        this.operation === 'Remove' ? null : [Validators.required, Validators.pattern('[A-Za-z0-9_-]+')]),
      httpMethod: new FormControl<HttpMethod>('GET', context.data.endpointType === 'HTTP' ? Validators.required : null)
    })
    this.httpMethods = context.data.queryKind === 'Query' ?
      ['GET', 'POST', 'PUT', 'DELETE'] :
      ['GET']
  }

  get pathIfExists(): string | null {
    return this.context.data.previousVersion.savedQuery?.httpEndpoint?.url ||
      this.context.data.previousVersion.savedQuery?.websocketOperation?.path;
  }

  get title(): string {
    const operation = this.operation === 'Add' ? 'Publish as' : 'Remove';
    return this.context.data.endpointType === 'HTTP' ? `${operation} HTTP API` : `${operation} Websocket API`
  }

  get operation(): Operation {
    const {previousVersion: {savedQuery}, endpointType} = this.context.data
    return (endpointType === 'HTTP' && savedQuery.httpEndpoint) || (endpointType === 'WEBSOCKET' && savedQuery.websocketOperation) ?
      'Remove' :
      'Add'
  }

  close() {
    this.context.completeWith(null);
  }

  update() {
    const versionedSource = this.context.data.previousVersion.sourceFile
    const schemaEditOperation = this.getSchemaEditOperation();
    const schemaEdit: SchemaEdit = {
      packageIdentifier: versionedSource.packageIdentifier,
      edits: [schemaEditOperation],
      dryRun: false
    }

    this.schemaImporterService.submitSchemaEditOperation(schemaEdit)
      .subscribe({
        next: (result) => {
          this.alerts.open('Query saved successfully', {status: TuiNotification.Success})
            .subscribe()
          const filename = this.schemaImporterService.extractFilenameFromVersionedSource(versionedSource)
          const updatedState =  this.schemaImporterService.getQueryStateFromEditResult(result, filename)
          this.context.completeWith(updatedState);
        },
        error: (error) => {
          console.error(error);
          this.errorMessage = error.error?.message || error.message;
          this.changeDetector.markForCheck();
        }
      })
  }

  private getSchemaEditOperation(): SchemaEditOperation {
    const queryQualifiedName = this.context.data.previousVersion.savedQuery.name;
    const prefix = this.context.data.endpointType === 'HTTP' ? this.httpPrefix : this.websocketPrefix;
    const path = this.operation === 'Add' ?
      prefix + this.formGroup.value.endpoint :
      this.pathIfExists;
    let operation: Operation = 'Add'
    if (this.context.data.endpointType === 'HTTP' && this.context.data.previousVersion.savedQuery.httpEndpoint ||
      this.context.data.endpointType === 'WEBSOCKET' && this.context.data.previousVersion.savedQuery.websocketOperation) {
      operation = 'Remove'
    }
    return this.context.data.endpointType === 'HTTP' ?
      {
        editKind: 'AddOrRemoveHttpEndpointAnnotation',
        queryQualifiedName,
        path,
        method: this.formGroup.value.httpMethod,
        operation
      } as AddOrRemoveHttpEndpointAnnotationEvent :
      {
        editKind: 'AddOrRemoveWebsocketEndpointAnnotation',
        queryQualifiedName,
        path,
        operation
      } as AddOrRemoveWebsocketEndpointAnnotationEvent
  }
}

type Operation = 'Add' | 'Remove'
