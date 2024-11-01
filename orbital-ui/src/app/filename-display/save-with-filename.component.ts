import { TuiLink } from "@taiga-ui/core";
import {Component, EventEmitter, Input, Output} from '@angular/core';
import {NgIf} from "@angular/common";
import {VersionedSource} from "../services/schema";

@Component({
  selector: 'app-save-with-filename',
  standalone: true,
  imports: [
    NgIf,
    TuiLink
  ],
  template: `
    <div class="filename" *ngIf="source">
      <span>
        <img src="assets/img/tabler/package.svg">
        {{ source.packageIdentifier.unversionedId }}
      </span>
      <span>
        <img src="assets/img/tabler/file-description.svg">
        {{ source.name }}
      </span>
    </div>
    <a
      *ngIf="showSaveButton"
      tuiLink
      class="button-link"
      (click)="saveFile.emit()"
      [class.disabled]="isDisabled"
    >
      <img class="primary" src="assets/img/tabler/device-floppy.svg">
    </a>
  `,
  styleUrl: './save-with-filename.component.scss'
})
export class SaveWithFilenameComponent {
  @Input()
  source: VersionedSource;

  @Input()
  isDisabled: boolean;

  @Input()
  showSaveButton: boolean = true;

  @Output()
  saveFile = new EventEmitter();
}
