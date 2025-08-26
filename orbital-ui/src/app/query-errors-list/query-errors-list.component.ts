import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  Inject,
  Input,
  LOCALE_ID
} from '@angular/core';
import {CommonModule, formatDate} from '@angular/common';
import {Observable, Subscription} from "rxjs";
import {StreamErrorMessage, StreamQueryErrorEvent} from "../services/query.service";
import {AgGridModule} from "ag-grid-angular";
import {ColDef} from "ag-grid-community";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {TextWithCopyCellRendererComponent} from "../shared/cell-renderers/text-with-copy-cell-renderer.component";

@Component({
  selector: 'app-query-errors-list',
  standalone: true,
  imports: [CommonModule, AgGridModule, TextWithCopyCellRendererComponent],
  template: `
    <ag-grid-angular
      style="width: 100%; height: 100%;"
      class="ag-theme-alpine"
      [rowData]="rowData"
      [columnDefs]="columnDefs">
    </ag-grid-angular>
  `,
  styleUrls: ['./query-errors-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QueryErrorsListComponent {
  constructor(private destroyRef: DestroyRef,
              private changeDetector: ChangeDetectorRef,
              @Inject(LOCALE_ID) private locale: string) {

  }

  private _errorMessages$: Observable<StreamQueryErrorEvent>
  private subscription: Subscription;

  @Input()
  get errorMessages$(): Observable<StreamQueryErrorEvent> {
    return this._errorMessages$;
  }

  set errorMessages$(value: Observable<StreamQueryErrorEvent>) {
    this._errorMessages$ = value;
    this.rowData = [];
    this.subscription?.unsubscribe()
    if (!this.errorMessages$)
      return;

    this.subscription = this.errorMessages$
      .pipe(
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(message => {
        this.rowData = [...this.rowData, message.error];
        this.changeDetector.markForCheck();
      });
  }


  public columnDefs: ColDef[] = [
    {
      field: 'timestamp',
      sortable: true,
      filter: true,
      resizable: true,
      valueFormatter: params => formatDate(params.value, 'medium', this.locale)
    },
    {
      field: 'message', 
      sortable: true, 
      filter: true, 
      resizable: true,
      cellRenderer: TextWithCopyCellRendererComponent
    },
    {field: 'typeName', sortable: true, filter: true, resizable: true},
    {
      field: 'payload', 
      sortable: true, 
      filter: true, 
      resizable: true,
      cellRenderer: TextWithCopyCellRendererComponent
    },
  ];

  public rowData: StreamErrorMessage[] = [];

  ngOnInit(): void {

  }
}
