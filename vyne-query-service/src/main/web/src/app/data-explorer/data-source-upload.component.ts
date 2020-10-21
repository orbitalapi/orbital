import {Component, ElementRef, EventEmitter, OnInit, Output, ViewChild} from '@angular/core';
import {UploadEvent, UploadFile} from 'ngx-file-drop';

@Component({
  selector: 'app-data-source-upload',
  template: `
      <!--    <div class="mat-elevation-z2 container">-->
      <!--      <img src="assets/img/upload.svg">-->
      <!--      <span>Drop your files here, or <a href="javascript:void;">click to browse</a></span>-->
      <!--    </div>-->
      <div class="container" #dropContainerInner>
          <file-drop (onFileDrop)="dropped($event)"
                     (onFileOver)="fileOver($event)"
                     (onFileLeave)="fileLeave($event)"
                     [multiple]="false"
                     browseBtnClassName="hidden-browse-button"
                     browseBtnLabel=""
                     [showBrowseBtn]="true"
                     dropZoneClassName="drop-container"
                     contentClassName="drop-container-content">
              <img src="assets/img/upload.svg">
              <span>Drop your files here.</span>
              <button mat-stroked-button (click)="browseClicked()">Browse</button>
          </file-drop>
      </div>

  `,
  styleUrls: ['./data-source-upload.component.scss']
})
export class DataSourceUploadComponent {

  @Output()
  fileSelected = new EventEmitter<UploadFile>();

  @ViewChild('dropContainerInner', {read: ElementRef, static: true}) tref: ElementRef;

  dropped(event: UploadEvent) {
    if (event.files.length === 1) {
      this.fileSelected.emit(event.files[0]);
    }

  }

  fileOver($event: any) {

  }

  fileLeave($event: any) {

  }

  browseClicked() {
    this.tref.nativeElement.querySelector('.hidden-browse-button').click();
  }
}
