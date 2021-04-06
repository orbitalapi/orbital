import { Component, OnInit, Inject } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material';

@Component({
  selector: 'app-cask-confirm-dialog',
  templateUrl: './cask-confirm-dialog.component.html',
  styleUrls: ['./cask-confirm-dialog.component.scss']
})
export class CaskConfirmDialogComponent implements OnInit {

  constructor(@Inject(MAT_DIALOG_DATA) public data: CaskConfirmDialogData) { }

  ngOnInit() {
  }

}

interface CaskConfirmDialogData  {
  title: string;
  message: string;
  messages?: string[];
}
