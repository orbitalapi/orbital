import {Component, Input, OnInit} from '@angular/core';
import {VersionedSource} from '../../services/schema';
import {HeaderTypes} from '../csv-viewer.component';

@Component({
  selector: 'app-schema-generator',
  templateUrl: './schema-generator.component.html',
  styleUrls: ['./schema-generator.component.scss']
})
export class SchemaGeneratorComponent implements OnInit {
  sources: VersionedSource[];
  @Input()
  headersWithAssignedTypes: HeaderTypes[];
  @Input()
  assignedTypeName: string;

  ngOnInit() {
    this.generateSchema();
  }

  generateSchema() {
    const assignedTypes = this.headersWithAssignedTypes.filter(item => item.typeName.length);
    const content = `model ${this.assignedTypeName} {
    ${assignedTypes.map((type, index) =>
      `${index > 0 ? '\n\t' : ''}${type.fieldName} : ${type.typeName}  ${type.format && `(@format = "${type.format && type.format}")`}`)}\n}`;

    this.sources = [{content: content, name: '', version: ''}];
  }
}
