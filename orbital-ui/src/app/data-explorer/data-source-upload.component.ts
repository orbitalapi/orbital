import {Component, ElementRef, EventEmitter, Input, Output, ViewChild} from '@angular/core';
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
                     dropZoneClassName="drop-container"
                     contentClassName="drop-container-content">
            <ng-template ngx-file-drop-content-tmp let-openFileSelector="openFileSelector">
              <div *ngIf="!mostRecentFile" class="drop-container-content">
                <img src="assets/img/tabler/cloud-upload.svg">
                <span>{{ promptText }}</span>
              </div>
              <div *ngIf="mostRecentFile" class="drop-container-content">
                <img src="assets/img/tabler/file.svg">
                <span>{{ mostRecentFile.fileEntry.name }}</span>
              </div>
              <button tuiButton appearance="whiteblock" size="m" (click)="openFileSelector()">Browse</button>
            </ng-template>

          </ngx-file-drop>
      </div>

  `,
  styleUrls: ['./data-source-upload.component.scss']
})
export class DataSourceUploadComponent {

  @Input()
  promptText = 'Drop your files here';

  @Output()
  fileSelected = new EventEmitter<NgxFileDropEntry>();

  mostRecentFile:NgxFileDropEntry;

  @ViewChild('dropContainerInner', {read: ElementRef, static: true}) tref: ElementRef;

  dropped(event: NgxFileDropEntry[]) {
    if (event.length === 1) {
      this.mostRecentFile = event[0];
      this.fileSelected.emit(event[0]);
    }

  }

  fileOver($event: any) {

  }

  fileLeave($event: any) {

  }
}
