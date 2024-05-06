import { CommonModule, NgOptimizedImage } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterLink } from '@angular/router';

import { HeaderComponentLayoutComponent } from './header-component-layout.component';

@NgModule({
  imports: [CommonModule, NgOptimizedImage, RouterLink],
  exports: [HeaderComponentLayoutComponent],
  declarations: [HeaderComponentLayoutComponent],
  providers: [],
})
export class HeaderComponentLayoutModule {
}
