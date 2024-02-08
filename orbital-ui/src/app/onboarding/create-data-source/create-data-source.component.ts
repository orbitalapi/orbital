import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-create-data-source',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './create-data-source.component.html',
  styleUrls: ['./create-data-source.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CreateDataSourceComponent {

}
