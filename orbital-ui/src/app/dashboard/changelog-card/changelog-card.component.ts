import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs/internal/Observable';
import { UiCustomisations } from '../../../environments/ui-customisations';
import { ChangeLogEntry, ChangelogService } from '../../changelog/changelog.service';
import { ProjectExplorerModule } from '../../project-explorer/project-explorer.module';
import { ParsedSource } from '../../services/schema';
import { SchemaNotificationService } from '../../services/schema-notification.service';
import { CardComponent } from '../card/card.component';

@Component({
  selector: 'app-changelog-card',
  standalone: true,
  imports: [CommonModule, CardComponent, ProjectExplorerModule],
  templateUrl: './changelog-card.component.html',
  styleUrls: ['./changelog-card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ChangelogCardComponent {
  schemas: Observable<ParsedSource[]>;
  changeLogEntries: Observable<ChangeLogEntry[]>
  protected readonly UiCustomisations = UiCustomisations;

  constructor(
    private changelogService: ChangelogService,
    private schemaNotificationService: SchemaNotificationService
  ) {
    this.loadSchemas();
    this.schemaNotificationService.createSchemaNotificationsSubscription().pipe(
      takeUntilDestroyed()
    )
      .subscribe(() => {
        this.loadSchemas();
      });
  }

  private loadSchemas() {
    this.changeLogEntries = this.changelogService.getChangelog();
  }

}
