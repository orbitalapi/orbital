import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Schema, Type, TypedInstance} from '../../services/schema';
import {ParsedTypeInstance, TypesService} from '../../services/types.service';
import {TypeNamedInstance} from '../../services/query.service';
import {FactForm} from '../query-wizard/query-wizard.component';

@Component({
  selector: 'app-file-fact-selector',
  templateUrl: './file-fact-selector.component.html',
  styleUrls: ['./file-fact-selector.component.scss']
})
export class FileFactSelectorComponent {

  @Input()
  schema: Schema;

  private _contentType: Type;

  selectedFile: File;

  fileContents: string;

  parsedInstance: ParsedTypeInstance;

  @Output()
  parsedInstanceChanged = new EventEmitter<ParsedTypeInstance>();

  @Output()
  typeSelected = new EventEmitter<Type>();

  @Input()
  get contentType(): Type {
    return this._contentType;
  }

  set contentType(value: Type) {
    this._contentType = value;
    this.typeSelected.emit(this.contentType);
    this.parseIfPossible();
  }

  constructor(private typesService: TypesService) {
  }


  onFileSelected(file: File): void {
    const reader = new FileReader();
    reader.onload = ((event) => {
      this.fileContents = (event.target as any).result;
      this.parseIfPossible();
      this.parsedInstanceChanged.emit(null);
    });
    reader.readAsText(file);
  }

  clearFile() {
    this.selectedFile = null;
    this.parsedInstance = null;
  }

  private parseIfPossible() {
    if (!this.contentType || !this.fileContents) {
      return;
    }
    this.typesService.parse(this.fileContents, this.contentType)
      .subscribe(event => {
        this.parsedInstance = event;
        this.parsedInstanceChanged.emit(this.parsedInstance);
      }, error => {
        console.error('Failed to parse instance: ' + error);
      });
  }
}
