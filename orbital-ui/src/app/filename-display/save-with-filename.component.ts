import {TuiLink} from "@taiga-ui/core";
import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from '@angular/core';
import {NgIf} from "@angular/common";
import {VersionedSource} from "../services/schema";
import {FilenameDisplayComponent} from "./filename-display.component";

@Component({
  selector: 'app-save-with-filename',
  standalone: true,
  imports: [
    NgIf,
    TuiLink,
    FilenameDisplayComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-filename-display [source]="source"  *ngIf="source"/>

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
