import {Component, EventEmitter, Input, Output} from '@angular/core';
import {VersionedSource} from "../services/schema";
import {NgIf} from "@angular/common";

@Component({
  selector: 'app-save-with-filename',
  standalone: true,
  imports: [
    NgIf
  ],
  template: `
    <a
      tuiLink
      class="button-link"
      (click)="saveFile.emit()"
    >
      <img class="primary" src="assets/img/tabler/device-floppy.svg">
    </a>
    <div class="filename" *ngIf="source">
      <img src="assets/img/tabler/package.svg">
      <span class="gap-right">{{ source.packageIdentifier.unversionedId }}</span>
      <img src="assets/img/tabler/file-description.svg">
      <span>{{ source.name }}</span>
    </div>

  `,
  styleUrl: './save-with-filename.component.scss'
})
export class SaveWithFilenameComponent {

  @Input()
  source: VersionedSource;

  @Output()
  saveFile = new EventEmitter();
}
