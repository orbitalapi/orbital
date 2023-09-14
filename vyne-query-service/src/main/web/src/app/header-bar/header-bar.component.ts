import {Component, Input, OnInit} from '@angular/core';
import {AuthService} from '../auth/auth.service';
import {UserInfoService, VyneUser} from '../services/user-info.service';

@Component({
    selector: 'app-header-bar',
    template: `
        <app-workspace-selector></app-workspace-selector>
        <div class="spacer"></div>
        <app-search-bar-container></app-search-bar-container>
        <app-avatar *ngIf="vyneUser" [user]="vyneUser"></app-avatar>
    `,
    styleUrls: ['./header-bar.component.scss']
})
export class HeaderBarComponent implements OnInit {
    vyneUser: VyneUser;

    constructor(private userInfoService: UserInfoService) {
    }

    @Input()
    title: string;

    ngOnInit(): void {
        this.userInfoService.userInfo$.subscribe(
            userInfo => this.vyneUser = userInfo,
            error => console.warn('Failed to retrieve user info - user is probably not logged in')
        );
    }

}
