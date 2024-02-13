import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TuiButtonModule } from '@taiga-ui/core';
import { UiCustomisations } from '../../../environments/ui-customisations';
import { SchemaImporterModule } from '../../schema-importer/schema-importer.module';

@Component({
  selector: 'app-create-data-source',
  standalone: true,
  imports: [CommonModule, RouterLink, TuiButtonModule, SchemaImporterModule],
  templateUrl: './create-data-source.component.html',
  styleUrls: ['./create-data-source.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CreateDataSourceComponent {
  readonly uiConfig = UiCustomisations;

  step: 'options' | 'dataSourceCreated' | 'swagger' | 'json' | 'database' | 'kafka' | 'protobuf' = 'options';
  hasCreatedDataSource: boolean;
}
