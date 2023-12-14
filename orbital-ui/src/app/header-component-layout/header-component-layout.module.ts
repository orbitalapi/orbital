import { NgModule } from '@angular/core';

import { HeaderComponentLayoutComponent } from './header-component-layout.component';
import {CommonModule, NgOptimizedImage} from '@angular/common';

@NgModule({
    imports: [CommonModule, NgOptimizedImage],
  exports: [HeaderComponentLayoutComponent],
  declarations: [HeaderComponentLayoutComponent],
  providers: [],
})
export class HeaderComponentLayoutModule {
}
