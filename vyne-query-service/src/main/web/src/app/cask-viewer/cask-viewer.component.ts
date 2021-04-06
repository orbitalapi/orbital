import { Component, OnInit } from '@angular/core';
import { CaskService, CaskConfigRecord } from '../services/cask.service';
import { MatDialog } from '@angular/material/dialog';
import { CaskConfirmDialogComponent } from './cask-confirm-dialog.component';

@Component({
  selector: 'app-cask-viewer',
  templateUrl: './cask-viewer.component.html',
  styleUrls: ['./cask-viewer.component.scss']
})
export class CaskViewerComponent implements OnInit {
  caskConfigs: {[type: string]: CaskConfigRecord[]};

  caskConfig: CaskConfigRecord;

  constructor(private service: CaskService, private dialog: MatDialog) { }

  ngOnInit() {
    this.loadCaskRecords();
  }

  showCaskDetails(caskConfig: CaskConfigRecord) {
    this.service.getCaskDetais(caskConfig.tableName).subscribe(details => {
      this.caskConfig = caskConfig;
      this.caskConfig.details = details;
     }
    );
  }

  resetCaskDetails() {
    this.caskConfig = null;
  }

  loadCaskRecords() {
    this.service.getCasks().subscribe( casks =>   {
      this.caskConfigs = casks.reduce((obj, caskConfig) => {
        const configs = obj[caskConfig.qualifiedTypeName] || [];
        configs.push(caskConfig);
        obj[caskConfig.qualifiedTypeName] = configs;
        return obj;
      }, {});
    } );
  }

  promptDeleteCask() {
    const deleteMessage = 'Are you sure you want to delete this cask?';
    const confirmationMessage = this.shouldForceDelete() ?
      `${deleteMessage} This cask has dependencies, which will also be deleted:` :
      deleteMessage;
    this.openConfirmDialog('Delete Cask', confirmationMessage, this.deleteCask, this.caskConfig.details.dependencies);
  }

  deleteCask = () => {
    this.service.deleteCask(this.caskConfig.tableName, this.shouldForceDelete()).subscribe( () => {
      this.resetCaskDetails();
      this.loadCaskRecords();
    } );
  }

  private shouldForceDelete(): boolean {
    return this.caskConfig.details.dependencies.length > 0;
  }

  openConfirmDialog(title: string, message: string, callback: () => void, messages: string[] = []) {
    const dialogRef = this.dialog.open(CaskConfirmDialogComponent, {
      data: {title: title, message: message, messages: messages}
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === true) {
        callback();
      }
    });
  }
}
