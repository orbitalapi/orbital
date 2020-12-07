import {Component, Input, OnInit} from '@angular/core';
import {Service} from '../services/schema';

@Component({
  selector: 'app-service-view-container',
  template: `
    <mat-toolbar color="primary">
      <span>Type Explorer</span>
      <span class="toolbar-spacer"></span>
      <app-search-bar-container></app-search-bar-container>
    </mat-toolbar>
    <app-service-view [service]="service"></app-service-view>
  `,
  styleUrls: ['./service-view-container.component.scss']
})
export class ServiceViewContainerComponent {

  @Input()
  service: Service;
}
