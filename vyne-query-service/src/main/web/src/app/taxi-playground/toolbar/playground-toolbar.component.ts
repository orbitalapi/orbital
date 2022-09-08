import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'playground-toolbar',
  template: `
    <img src="assets/img/taxi-icon.svg" class="logo">
    <span class="title">Taxi playground</span>
    <div class="spacer"></div>
    <button class="secondary">Save your design in Orbital</button>

  `,
  styleUrls: ['./playground-toolbar.component.scss']
})
export class PlaygroundToolbarComponent implements OnInit {

  constructor() { }

  ngOnInit(): void {
  }

}
