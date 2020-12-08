import {Component, Input, OnInit} from '@angular/core';
import {Service} from '../services/schema';
import {TypesService} from '../services/types.service';
import {ActivatedRoute, ParamMap, Router} from '@angular/router';
import {flatMap, map} from 'rxjs/operators';

@Component({
  selector: 'app-service-view-container',
  template: `
    <mat-toolbar color="primary">
      <span>Service Explorer</span>
      <span class="toolbar-spacer"></span>
      <app-search-bar-container></app-search-bar-container>
    </mat-toolbar>
    <app-service-view [service]="service"></app-service-view>
  `,
  styleUrls: ['./service-view-container.component.scss']
})
export class ServiceViewContainerComponent implements OnInit {

  @Input()
  service: Service;

  constructor(private typeService: TypesService, private activeRoute: ActivatedRoute) {
  }

  ngOnInit(): void {
    this.activeRoute.paramMap.pipe(
      map((params: ParamMap) => params.get('serviceName')),
      flatMap(serviceName => this.typeService.getService(serviceName))
    ).subscribe((service: Service) => {
      this.service = service;
    });
  }
}
