import { AsyncPipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { TuiButtonModule } from '@taiga-ui/core';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { UiCustomisations } from '../../environments/ui-customisations';
import { HeaderComponentLayoutModule } from '../header-component-layout/header-component-layout.module';

@Component({
  selector: 'app-data-source-manager-header',
  template: `
    <app-header-component-layout
      title="Data sources"
      [description]="'Create data sources and connections to register databases and message brokers to ' + UiCustomisations.productName"
      [fullWidth]="isConfiguringDataSource$ | async"
    >
      <ng-container ngProjectAs="buttons">
        <button
          tuiButton
          size="m"
          icon="tuiIconPlus"
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
    HeaderComponentLayoutModule,
    TuiButtonModule,
    RouterOutlet,
    AsyncPipe,
    NgIf
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
