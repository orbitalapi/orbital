import { TuiButton } from "@taiga-ui/core";
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UiCustomisations } from '../../../environments/ui-customisations';
import { DataSourceImportComponent } from '../../data-source-import/data-source-import.component';

@Component({
  selector: 'app-select-data-source',
  standalone: true,
  imports: [CommonModule, RouterLink, TuiButton, DataSourceImportComponent],
  templateUrl: './data-source.component.html',
  styleUrls: ['./data-source.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DataSourceComponent {
  readonly uiConfig = UiCustomisations;

  step: 'options' | 'dataSourceSelected' | 'dataSourceConfigured' | 'swagger' | 'json' | 'database' | 'kafka' | 'protobuf' = 'options';

  constructor(private router: Router, private activatedRoute: ActivatedRoute,) {
  }

  onDataSourceSelected() {
    this.router.navigate(['configure'], { relativeTo: this.activatedRoute, replaceUrl: true });
  }
}
