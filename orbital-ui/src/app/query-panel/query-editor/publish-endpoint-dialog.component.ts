import {Component, Inject} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {TuiAlertService, TuiDialogContext, TuiNotification} from '@taiga-ui/core';
import {POLYMORPHEUS_CONTEXT} from "@tinkoff/ng-polymorpheus";
import {
  AddHttpEndpointToQueryEvent,
  AddWebsocketEndpointToQueryEvent,
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
      [title]="context.data.endpointType === 'HTTP' ? 'Publish as HTTP API' : 'Publish as Websocket API'">
      <form [formGroup]="formGroup">
        <tui-input
          formControlName="endpoint"
          [tuiTextfieldPrefix]="context.data.endpointType === 'HTTP' ? httpPrefix : websocketPrefix"
          [pseudoFocus]="true"
          tuiAutoFocus
        >
          API Endpoint
          <input
            tuiTextfield
          />
        </tui-input>
        <tui-select
          *ngIf="context.data.endpointType === 'HTTP'"
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
          appearance="primary"
          [disabled]="!formGroup.valid"
          (click)="update()"
        >
          Update
        </button>
      </div>
    </app-header-component-layout>
  `,
  styleUrls: ['./publish-endpoint-dialog.component.scss']
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
  ) {
    this.formGroup = new FormGroup({
      endpoint: new FormControl(null,
        [Validators.required, Validators.pattern('[A-Za-z0-9_-]+')]),
      httpMethod: new FormControl<HttpMethod>('GET', context.data.endpointType === 'HTTP' ? Validators.required : null)
    })
    this.httpMethods = context.data.queryKind === 'Query' ?
      ['GET', 'POST', 'PUT', 'DELETE'] :
      ['GET']
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
        }
      })
  }

  private getSchemaEditOperation(): SchemaEditOperation {
    const queryQualifiedName = this.context.data.previousVersion.savedQuery.name;
    const prefix = this.context.data.endpointType === 'HTTP' ? this.httpPrefix : this.websocketPrefix;
    const path = prefix + this.formGroup.value.endpoint;
    return this.context.data.endpointType === 'HTTP' ?
      {
        editKind: 'AddHttpEndpointToQuery',
        queryQualifiedName,
        path,
        method: this.formGroup.value.httpMethod
      } as AddHttpEndpointToQueryEvent :
      {
        editKind: 'AddWebsocketEndpointToQuery',
        queryQualifiedName,
        path
      } as AddWebsocketEndpointToQueryEvent
  }
}
