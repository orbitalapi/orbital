import {Component, Input} from '@angular/core';
import { SourcePackageDescription } from "src/app/package-viewer/packages.service";
import { AsyncPipe, KeyValuePipe, NgForOf, NgIf } from "@angular/common";

@Component({
  selector: 'app-project-problem-list',
  standalone: true,
  imports: [
    NgForOf,
    NgIf,
    KeyValuePipe
  ],
  template: `
    <div *ngIf="hasConfigErrors(); else noErrors">
      <div>Problems were detected with the config files in this project</div>
      <div class="project-list-container">
        <table class="project-list">
          <thead>
          <tr>
            <th>File</th>
            <th>Error message</th>
          </tr>
          </thead>
          <tbody>
          <tr *ngFor="let message of packageDescription?.configurationFileErrors | keyvalue">
            <td>{{ message.key }}</td>
            <td>{{ message.value }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>
    <ng-template #noErrors>
      <div class="empty-state">No configuration errors found</div>
    </ng-template>
  `,
  styleUrl: './project-problem-list.component.scss'
})
export class ProjectProblemListComponent {
  @Input() packageDescription!: SourcePackageDescription;

  hasConfigErrors(): boolean {
    return this.packageDescription?.configurationFileErrors != null &&
      Object.keys(this.packageDescription.configurationFileErrors).length > 0;
  }
}
