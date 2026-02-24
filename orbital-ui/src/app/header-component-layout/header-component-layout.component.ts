import { Component, HostBinding, Input, TemplateRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-header-component-layout',
  standalone: true,
  imports: [CommonModule, RouterLink],
  styleUrls: ['./header-component-layout.component.scss'],
  template: `
    <div class="header-container" [ngClass]="{'pad-bottom' : padBottom}">
      <div class="header">
        <div class="row">
          <a *ngIf="backLink" class="back-link filter-link-color" [routerLink]="backLink"><img src="assets/img/tabler/arrow-left.svg"></a>
          <img class="icon" *ngIf="iconUrl" [attr.src]="iconUrl">
          <div class="header-text">
            <h4 *ngIf="subtitle">{{ subtitle}}</h4>
            <h2>
              <ng-container *ngIf="isString; else template">{{ title }}</ng-container>
              <ng-template #template><ng-container *ngTemplateOutlet="title"></ng-container></ng-template>
            </h2>
            <p class="description">{{ description }}</p>
          </div>
          <div class="spacer"></div>
          <div class="buttons">
            <ng-content select="buttons">
            </ng-content>
          </div>
        </div>
        <ng-content select="header-components"></ng-content>
      </div>
    </div>
    <div class="body-container" *ngIf="displayBody">
      <div class="body" [ngClass]="{'full-width' : fullWidth}">
        <ng-content></ng-content>
      </div>
    </div>
  `
})
export class HeaderComponentLayoutComponent {

  @Input()
  backLink: string;

  @Input()
  subtitle: string = null;

  @Input()
  iconUrl: string;

  @Input()
  title: string | TemplateRef<any>;
  // Prevents tooltip displaying in browser
  @HostBinding('attr.title') get getTitle(): null {
    return null;
  }

  @Input()
  description: string;

  /**
   * If passing something like a tab bar to the header components,
   * then set padBottom to false
   */
  @Input()
  padBottom: boolean = true;

  /**
   * Set to false to hide the body container, thus making this a header-only
   * display.
   *
   * Useful if you want a consistent header, but full-width content
   */
  @Input()
  displayBody = true;

  /**
   * Allow the body to expand to full width.
   * Otherwise, uses a comfortable width of 1280px or so.
   */
  @Input()
  fullWidth: boolean = false;

  get isString(): boolean {
    return typeof this.title === 'string';
  }
}
