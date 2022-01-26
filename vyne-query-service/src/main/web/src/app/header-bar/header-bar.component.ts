import {Component, Input, OnInit} from '@angular/core';
import {AuthService} from '../auth/auth.service';
import {UserInfoService, VyneUser} from '../services/user-info.service';

@Component({
  selector: 'app-header-bar',
  template: `
    <mat-toolbar color="primary" style="z-index: 999">
      <span class="page-title">{{title}}</span>
      <ng-content></ng-content>
      <div class="toolbar-spacer"></div>
      <app-search-bar-container></app-search-bar-container>
      <app-avatar *ngIf="vyneUser && vyneUser.isAuthenticated" [user]="vyneUser"></app-avatar>
    </mat-toolbar>`,
  styleUrls: ['./header-bar.component.scss']
})
export class HeaderBarComponent implements OnInit {
  vyneUser: VyneUser;

  constructor(private userInfoService: UserInfoService) {
  }

  @Input()
  title: string;

  ngOnInit(): void {
    this.userInfoService.getUserInfo().subscribe(
      userInfo => this.vyneUser = userInfo,
      error => console.warn('Failed to retrieve user info - user is probably not logged in')
    );
  }

}
