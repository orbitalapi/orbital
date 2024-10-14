import {CommonModule} from '@angular/common';
import {Component, Input} from '@angular/core';
import {Observable} from 'rxjs/internal/Observable';
import {ChangelogTimelineComponent} from '../changelog/changelog-timeline.component';
import {ParsedSource} from '../services/schema';
import {appInstanceType} from 'src/app/app-config/app-instance.vyne';
import {UiCustomisations} from '../../environments/ui-customisations';

@Component({
    selector: 'app-project-summary-view',
    template: `
    <div class="container">
      <ng-container *ngIf="displayHeaderText">
        <h1>Changelog</h1>
        <p>This is the changelog of all the data sources connected to {{ UiCustomisations.productName }}</p>
        <p>Click on a package on the left to view the schema for that package</p>
      </ng-container>
      <app-changelog-timeline></app-changelog-timeline>
    </div>
  `,
    styleUrls: ['./project-summary-view.component.scss'],
    host: { 'class': appInstanceType.appType },
    standalone: true,
    imports: [CommonModule, ChangelogTimelineComponent]
})
export class ProjectSummaryViewComponent {
  @Input()
  displayHeaderText: boolean = true;

  schemas: Observable<ParsedSource[]>;
  protected readonly UiCustomisations = UiCustomisations;

}
