import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-header-component-layout',
  styleUrls: ['./header-component-layout.component.scss'],
  template: `
    <div class="header-container" [ngClass]="{'pad-bottom' : padBottom}">
      <div class="header">
        <div class="row">
          <img class="icon" *ngIf="iconUrl" [attr.src]="iconUrl">
          <div class="header-text">
            <h4 *ngIf="subtitle">{{ subtitle}}</h4>
            <h2>{{ title }}</h2>
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
    <div class="body-container" *ngIf='displayBody'>
      <div class="body" [ngClass]="{'full-width' : fullWidth}">
        <ng-content></ng-content>
      </div>
    </div>
  `
})
export class HeaderComponentLayoutComponent {

  @Input()
  showBack: boolean = false;

  @Input()
  subtitle: string = null;

  @Input()
  iconUrl: string;

  @Input()
  title: string;

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
}
