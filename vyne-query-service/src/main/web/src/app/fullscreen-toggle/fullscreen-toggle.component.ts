import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

@Component({
  selector: 'app-fullscreen-toggle',
  template: `
    <button mat-mini-fab color="primary" (click)="toggleFullscreen()" class="fullscreen-toggle">
      <mat-icon svgIcon="fullscreen-on" *ngIf="!fullscreen"></mat-icon>
      <mat-icon svgIcon="fullscreen-off" *ngIf="fullscreen"></mat-icon>
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
