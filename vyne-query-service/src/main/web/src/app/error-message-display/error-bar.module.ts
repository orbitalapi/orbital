import {NgModule} from '@angular/core';

import {ErrorBarComponent} from './error-bar.component';
import {MatIconModule} from '@angular/material/icon';

@NgModule({
  imports: [
    MatIconModule
  ],
  exports: [ErrorBarComponent],
  declarations: [ErrorBarComponent],
  providers: [],
})
export class ErrorBarModule {
}
