import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-simple-badge-list',
  styleUrls: ['./simple-badge-list.component.scss'],
  template: `
    <div *ngFor="let badge of badges" class="icon-badge">
      <img [attr.src]="badge.iconPath" class="filter-black-ish">
      <span class="label">{{ badge.label }} : </span>
      <span class="value">{{ badge.value }}</span>
    </div>
  `
})
export class SimpleBadgeListComponent {

  @Input()
  badges: Badge[]
}

export interface Badge {
  label: string;
  value: string;
  iconPath: string | null;
}
