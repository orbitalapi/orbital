import {Component, Input, OnInit} from '@angular/core';

@Component({
  selector: 'app-file-extension-icon',
  template: `
    <div class="outer-container">
      <div class="container">
        <img src="assets/img/file-with-format.svg">
        <span>{{extension}}</span>
      </div>
    </div>

  `,
  styles: [`
      .outer-container {
          width: 34px;
          height: 34px;
      }

    .container {
          position: relative;
      }

      img {
          position: absolute;
          width: 34px;
      }

      span {
          position: absolute;
          top: 9px;
          left: 5px;
          font-size: 10px;
          color: white;
          font-weight: bold;
      }
  `]
})
export class FileExtensionIconComponent {
  @Input()
  extension = '';

}
