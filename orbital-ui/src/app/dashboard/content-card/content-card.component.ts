import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {CardComponent} from "../card/card.component";
import {NgIf} from "@angular/common";
import {RouterLink} from "@angular/router";
import {ContentCardData} from "../../services/content.service";

@Component({
  selector: 'app-content-card',
  standalone: true,
  imports: [
    CardComponent,
    NgIf,
    RouterLink
  ],
  template: `
    <app-card *ngIf="cardData"
              [title]="cardData.title"
              [icon]="cardData.iconUrl"
    >
      <div class="body-text">{{ cardData.text }}</div>
      <div class="button-bar">
        <a class="link" *ngIf="!cardData.action.external"
           [routerLink]="cardData.action.route">{{ cardData.action.text }}</a>
        <a class="link" *ngIf="cardData.action.external" [href]="cardData.action.route[0]" target="_blank">
          <span>{{ cardData.action.text }}</span>
          <img src="assets/img/tabler/external-link.svg">
        </a>
      </div>
    </app-card>
  `,
  styleUrl: './content-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ContentCardComponent {

  @Input()
  cardData: ContentCardData;
}

