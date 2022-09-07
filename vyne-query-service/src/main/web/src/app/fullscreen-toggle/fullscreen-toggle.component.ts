import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

@Component({
  selector: 'app-fullscreen-toggle',
  template: `
    <button mat-icon-button (click)="toggleFullscreen()" class="fullscreen-toggle">
      <img src="assets/img/full-screen-on.svg" *ngIf="!fullscreen">
      <img src="assets/img/full-screen-off.svg" *ngIf="fullscreen">
    </button>
  `,
  styleUrls: ['./fullscreen-toggle.component.scss']
})
export class FullscreenToggleComponent {

  @Input()
  fullscreen: boolean = false;

  @Output()
  fullscreenChange = new EventEmitter<boolean>();

  toggleFullscreen() {
    this.fullscreen = !this.fullscreen
    this.fullscreenChange.emit(this.fullscreen);
  }
}
