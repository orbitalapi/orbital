import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-header-component-layout',
  styleUrls: ['./header-component-layout.component.scss'],
  template: `
      <div class="header-container" [ngClass]="{'pad-bottom' : padBottom}">
          <div class="header">
              <div class="row">
                  <div class="header-text">
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
      <div class="body-container">
          <div class="body">
              <ng-content></ng-content>
          </div>
      </div>
  `
})
export class HeaderComponentLayoutComponent {

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
}
