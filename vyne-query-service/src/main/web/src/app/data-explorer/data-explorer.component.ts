import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Schema, Type} from '../services/schema';
import {ParsedTypeInstance, TypesService} from '../services/types.service';
import {FileSystemEntry, FileSystemFileEntry, UploadFile} from 'ngx-file-drop';

@Component({
  selector: 'app-data-explorer',
  templateUrl: './data-explorer.component.html',
  styleUrls: ['./data-explorer.component.scss']
})
export class DataExplorerComponent {

  schema: Schema;
  fileContents: string;
  private _contentType: Type;
  parsedInstance: ParsedTypeInstance;

  @Output()
  parsedInstanceChanged = new EventEmitter<ParsedTypeInstance>();

  constructor(private typesService: TypesService) {
    this.typesService.getTypes()
      .subscribe(next => this.schema = next);
  }

  @Input()
  get contentType(): Type {
    return this._contentType;
  }

  set contentType(value: Type) {
    this._contentType = value;
    this.parseIfPossible();
  }

  onFileSelected(uploadFile: UploadFile): void {
    if (!uploadFile.fileEntry.isFile) {
      throw new Error('Only files are supported');
    }

    const fileEntry = uploadFile.fileEntry as FileSystemFileEntry;
    fileEntry.file(file => {
      const reader = new FileReader();
      reader.onload = ((event) => {
        this.fileContents = (event.target as any).result;
        this.parseIfPossible();
        this.parsedInstanceChanged.emit(null);
      });
      reader.readAsText(file);
    });


  }

  clearFile() {
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
