import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CaskViewerComponent } from './cask-viewer.component';
import { CaskRowComponent } from './cask-row.component';
import { VyneServicesModule } from '../services/vyne-services.module';
import { SearchModule } from '../search/search.module';
import { MatExpansionModule } from '@angular/material/expansion';
import { CodeViewerModule } from '../code-viewer/code-viewer.module';
import { CaskDetailsComponent } from './cask-details.component';
import { MatDialogModule } from '@angular/material/dialog';
import { CaskConfirmDialogComponent } from './cask-confirm-dialog.component';
import { CaskSourceViewerComponent } from './cask-source-viewer.component';
import {
  CaskIngestionErrorsSearchPanelComponent
} from './cask-ingestion-errors/cask-ingestion-errors-search-panel/cask-ingestion-errors-search-panel.component';
import { CaskIngestionErrorsComponent } from './cask-ingestion-errors/cask-ingestion-errors.component';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { AgGridModule } from 'ag-grid-angular';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import {
  CaskIngestionErrorsGridComponent
} from './cask-ingestion-errors/cask-ingestion-errors-grid/cask-ingestion-errors-grid.component';
import { HeaderBarModule } from '../header-bar/header-bar.module';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { RouterModule } from '@angular/router';
import { AuthGuard } from 'src/app/services/auth.guard';
import { VynePrivileges } from 'src/app/services/user-info.service';


@NgModule({
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatExpansionModule,
    MatToolbarModule,
    VyneServicesModule,
    SearchModule,
    CodeViewerModule,
    MatTooltipModule,
    MatDialogModule,
    MatFormFieldModule,
    MatSelectModule,
    MatDatepickerModule,
    MatIconModule,
    MatButtonModule,
    MatInputModule,
    AgGridModule,
    ReactiveFormsModule,
    FormsModule,
    HeaderBarModule,
    MatListModule,
    RouterModule.forChild([
      {
        path: '',
        component: CaskViewerComponent,
      },
    ])
  ],
  exports: [CaskViewerComponent, CaskRowComponent, CaskDetailsComponent],
  declarations: [CaskViewerComponent,
    CaskRowComponent,
    CaskDetailsComponent,
    CaskConfirmDialogComponent,
    CaskSourceViewerComponent,
    CaskIngestionErrorsSearchPanelComponent,
    CaskIngestionErrorsComponent,
    CaskIngestionErrorsGridComponent],
  providers: [],
  entryComponents: [CaskConfirmDialogComponent]
})
export class CaskViewerModule {
}
