import {AuthService} from './auth.service';
import {Component, OnInit} from '@angular/core';
import {MatDialog} from '@angular/material/dialog';
import {CaskConfirmDialogComponent} from '../cask-viewer/cask-confirm-dialog.component';
import {Router} from "@angular/router";

@Component({
  template: ''
})

export class LogoutComponent implements OnInit {

  constructor(private authService: AuthService, private dialog: MatDialog, private router: Router) {}

  ngOnInit() {
   this.showConfirmationDialog();
  }

  private showConfirmationDialog() {
    const dialogRef = this.dialog.open(CaskConfirmDialogComponent, {
      data: {title: 'Logout', message: 'Are you sure?'}
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === true) {
        this.authService.logout();
      } else {
        this.router.navigate(['/query-wizard']);
      }
    });
  }
}
