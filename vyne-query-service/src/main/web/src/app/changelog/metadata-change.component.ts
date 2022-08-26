import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { Metadata, QualifiedName } from '../services/schema';

@Component({
  selector: 'app-metadata-change',
  template: `
    <div *ngFor="let metadataName of metadataNames">
      <h4>{{ metadataName.shortDisplayName }}</h4>
      <table class="table">
        <thead>
        <tr>
          <th>Property name</th>
          <th>Old value</th>
          <th>New value</th>
        </tr>
        </thead>
        <tbody>
        <tr *ngFor="let row of buildMetadataTable(metadataName)">
          <td>{{row.name}}</td>
          <td>{{row.oldValue}}</td>
          <td>{{row.newValue}}</td>
        </tr>
        </tbody>
      </table>
    </div>

  `,
  styleUrls: ['./metadata-change.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MetadataChangeComponent {

  @Input()
  oldValue: Metadata[];

  @Input()
  newValue: Metadata[];

  buildMetadataTable(name: QualifiedName) {
    const oldMetadata = this.getMetadata(this.oldValue, name)?.params || {};
    const newMetadata = this.getMetadata(this.newValue, name)?.params || {};

    const propertyNames = Array.from(new Set(
      Object.keys(oldMetadata).concat(Object.keys(newMetadata))
    ))

    return propertyNames.map(propertyName => {
      return { name: propertyName, oldValue: oldMetadata[propertyName], newValue: newMetadata[propertyName] }
    })
  }


  getMetadata(collection: Metadata[], name: QualifiedName): Metadata | null {
    return collection.find(m => m.name.parameterizedName === name.parameterizedName);
  }

  get metadataNames(): QualifiedName[] {
    if (!this.oldValue || !this.newValue) {
      return [];
    }
    const distinctMetadataNames: QualifiedName[] = this.oldValue
      .concat(this.newValue.filter(q => {
        // Filter newValue metadata to those not present in oldValue
        return this.oldValue.findIndex(s => s.name.parameterizedName) === -1
      }))
      .map(m => m.name)
    return distinctMetadataNames;
  }
}
