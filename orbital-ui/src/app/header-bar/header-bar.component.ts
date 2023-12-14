import {Component, Input, OnInit} from '@angular/core';
import {UserInfoService, VyneUser} from '../services/user-info.service';
import {AppConfig, AppInfoService} from "../services/app-info.service";
import {Observable} from "rxjs";

@Component({
  selector: 'app-header-bar',
  template: `
      <app-workspace-selector *ngIf="(appConfig$ | async)?.featureToggles.workspacesEnabled"></app-workspace-selector>
      <div class="spacer"></div>
      <app-search-bar-container></app-search-bar-container>
      <app-avatar *ngIf="vyneUser" [user]="vyneUser"></app-avatar>
  `,
  styleUrls: ['./header-bar.component.scss']
})
export class HeaderBarComponent implements OnInit {
  vyneUser: VyneUser;
  appConfig$: Observable<AppConfig>;

  constructor(private userInfoService: UserInfoService, private appConfigService: AppInfoService) {
    this.appConfig$ = this.appConfigService.getConfig()
  }

  @Input()
  title: string;

  ngOnInit(): void {
    this.userInfoService.userInfo$.subscribe(
      userInfo => {
        this.vyneUser = userInfo
      },
      error => console.warn('Failed to retrieve user info - user is probably not logged in')
    );
  }

}
