import {Component, ElementRef, EventEmitter, OnInit, Output, ViewChild} from '@angular/core';
import {NgxFileDropEntry} from 'ngx-file-drop';

@Component({
  selector: 'app-data-source-upload',
  template: `
      <!--    <div class="mat-elevation-z2 container">-->
      <!--      <img src="assets/img/upload.svg">-->
      <!--      <span>Drop your files here, or <a href="javascript:void;">click to browse</a></span>-->
      <!--    </div>-->
      <div class="container" #dropContainerInner>
          <ngx-file-drop (onFileDrop)="dropped($event)"
                     (onFileOver)="fileOver($event)"
                     (onFileLeave)="fileLeave($event)"
                     [multiple]="false"
                     browseBtnClassName="hidden-browse-button"
                     browseBtnLabel=""
                     [showBrowseBtn]="true"
                     dropZoneClassName="drop-container"
                     contentClassName="drop-container-content">
            <ng-template ngx-file-drop-content-tmp let-openFileSelector="openFileSelector">
              <img src="assets/img/upload.svg">
              <span>Drop your files here</span>
              <button mat-stroked-button (click)="openFileSelector()">Browse</button>
            </ng-template>

          </ngx-file-drop>
      </div>

  `,
  styleUrls: ['./data-source-upload.component.scss']
})
export class DataSourceUploadComponent {

  @Output()
  fileSelected = new EventEmitter<NgxFileDropEntry>();

  @ViewChild('dropContainerInner', {read: ElementRef, static: true}) tref: ElementRef;

  dropped(event: NgxFileDropEntry[]) {
    if (event.length === 1) {
      this.fileSelected.emit(event[0]);
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
