import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {LandingPageCardConfig} from './landing-page.component';

@Component({
  selector: 'app-landing-card',
  template: `
    <h4>{{ cardConfig.title}}</h4>
    <div *ngIf="isEmpty" class="empty-container" [ngClass]="layout">
      <img [src]="cardConfig.emptyStateImage">
      <div class="empty-container-content">
        <p>{{ cardConfig.emptyText }}</p>
        <button tuiButton shape="rounded" (click)="emptyActionClicked.emit()">{{ cardConfig.emptyActionLabel }}</button>
      </div>
    </div>
  `,
  styleUrls: ['./landing-card.component.scss']
})
export class LandingCardComponent {

  @Input()
  isEmpty: boolean

  @Input()
  cardConfig: LandingPageCardConfig

  @Output()
  emptyActionClicked = new EventEmitter();

  @Input()
  layout: 'horizontal' | 'vertical' = 'horizontal'

}
