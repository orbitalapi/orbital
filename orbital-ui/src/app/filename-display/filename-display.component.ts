import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {VersionedSource} from "../services/schema";
import {NgIf} from "@angular/common";

@Component({
  selector: 'app-filename-display',
  standalone: true,
  imports: [
    NgIf
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
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './filename-display.component.scss'
})
export class FilenameDisplayComponent {

  @Input()
  source: VersionedSource;


}
