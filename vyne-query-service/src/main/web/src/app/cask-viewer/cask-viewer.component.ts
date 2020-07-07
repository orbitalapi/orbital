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
  caskConfigs: {[type: string]: CaskConfigRecord[]}

  caskConfig: CaskConfigRecord

  constructor(private service: CaskService, private dialog: MatDialog) { }

  ngOnInit() {
    this.loadCaskRecords();
  }

  showCaskDetails(caskConfig: CaskConfigRecord) {
    this.service.getCaskDetais(caskConfig.tableName).subscribe(details => {
      this.caskConfig = caskConfig
      this.caskConfig.details = details
     }
    )
  }

  resetCaskDetails() {
    this.caskConfig = null
  }

  loadCaskRecords() {
    this.service.getCasks().subscribe( casks =>   {
      this.caskConfigs = casks.reduce((obj, caskConfig) => {
        const configs = obj[caskConfig.qualifiedTypeName] || []
        configs.push(caskConfig)
        obj[caskConfig.qualifiedTypeName] = configs
        return obj
      },{})
    } )
  }

  promptDeleteCask() {
    this.openConfirmDialog('Delete Cask', 'Are you sure you want to delete this cask ?', this.deleteCask);
  }

  deleteCask = () => {
    this.service.deleteCask(this.caskConfig.tableName).subscribe( () => {
      this.resetCaskDetails()
      this.loadCaskRecords()
    } )
  }

  promptClearCask() {
    this.openConfirmDialog('Clear Cask', 'Are you sure you clear all data in this cask ?', this.clearCask);
  }

  clearCask = () => {
    this.service.clearCask(this.caskConfig.tableName).subscribe(() => {
      this.showCaskDetails(this.caskConfig)
    })
  }

  openConfirmDialog(title: string, message:string, callback: () => void) {
    const dialogRef = this.dialog.open(CaskConfirmDialogComponent,{
      data: {title: title, message: message}
    });

    dialogRef.afterClosed().subscribe(result => {
      if(result == true) {
        callback()
      }
    });
  }

}
