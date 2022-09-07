import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FullscreenToggleComponent } from './fullscreen-toggle.component';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';


@NgModule({
  declarations: [
    FullscreenToggleComponent
  ],
  exports: [
    FullscreenToggleComponent
  ],
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule
  ]
})
export class FullscreenToggleModule {
}
