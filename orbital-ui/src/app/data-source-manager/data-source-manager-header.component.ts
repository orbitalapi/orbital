import { TuiButton } from "@taiga-ui/core";
import { AsyncPipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { UiCustomisations } from '../../environments/ui-customisations';
import { HeaderComponentLayoutComponent } from '../header-component-layout/header-component-layout.component';
import {RequiresAuthorityDirective} from "../requires-authority.directive";

@Component({
  selector: 'app-data-source-manager-header',
  template: `
    <app-header-component-layout
      title="Data sources"
      [description]="'Create data sources and connections to register databases and message brokers to ' + UiCustomisations.productName"
      [fullWidth]="isConfiguringDataSource$ | async"
    >
      <ng-container ngProjectAs="buttons">
        <button *appRequiresAuthority="['EditConnections']"
          tuiButton
          size="m"
          iconStart="@tui.plus"
          appearance="primary"
          [disabled]="(isAddingDataSource$ | async)"
          (click)="addDataSource()"
        >
          Add data source
        </button>
      </ng-container>
      <router-outlet></router-outlet>
    </app-header-component-layout>
  `,
  styleUrls: ['./data-source-manager-header.component.scss'],
  imports: [
    HeaderComponentLayoutComponent,
    TuiButton,
    RouterOutlet,
    AsyncPipe,
    NgIf,
    RequiresAuthorityDirective
  ],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DataSourceManagerHeaderComponent implements OnInit {
  isAddingDataSource$: Observable<boolean>;
  isConfiguringDataSource$: Observable<boolean>

  constructor(private router: Router) {
  }

  ngOnInit(): void {
    this.isAddingDataSource$ = this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        map((event: NavigationEnd) => event.urlAfterRedirects.split('/')?.[2]?.includes('add')),
      )
    this.isConfiguringDataSource$ = this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        map((event: NavigationEnd) => event.urlAfterRedirects.split('/')?.[3]?.includes('configure')),
      )
  }

  addDataSource() {
    this.router.navigate(['data-source-manager', 'add'] );
  }

  protected readonly UiCustomisations = UiCustomisations;
}
