import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {CompilationMessage} from '../services/schema';

@Component({
  selector: 'app-error-list',
  styleUrls: ['./error-list.component.scss'],
  template: `
    <div class="header">
      Problems
    </div>
    <table>
      <tr *ngFor="let error of errors">
        <td class="severity-cell" [ngClass]="error.severity.toLowerCase()">
          <img [attr.src]="getSeverityIcon(error.severity)" class="filter-error-light">
        </td>
        <td class="description-cell">{{ error.detailMessage }}</td>
        <td class="position-cell">[{{error.line}},{{error.char}}]</td>
      </tr>
    </table>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ErrorListComponent {

  @Input()
  errors: CompilationMessage[];

  getSeverityIcon(severity: "INFO" | "WARNING" | "ERROR"): string {
    const path = `assets/img/tabler`;
    switch (severity) {
      case "ERROR":
        return `${path}/circle-x.svg`;
      case "WARNING":
        return `${path}/alert-triangle.svg`;
      case "INFO":
        return `${path}/info-circle.svg`
    }
  }
}
