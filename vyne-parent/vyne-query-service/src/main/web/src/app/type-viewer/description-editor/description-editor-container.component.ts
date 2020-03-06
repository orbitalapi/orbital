import {Component, Input} from '@angular/core';
import {Type} from '../../services/schema';
import {TypesService} from '../../services/types.service';
import {MatSnackBar} from '@angular/material/snack-bar';

@Component({
  selector: 'app-description-editor-container',
  template: `
    <app-description-editor [type]="type" (save)="doSave($event)"
                            (cancelEdits)="cancelEdits()"></app-description-editor>
  `,
  styleUrls: ['./description-editor.component.scss']
})
export class DescriptionEditorContainerComponent {

  originalTypeDoc: string | null;
  loading = false;

  private _type: Type;
  @Input()
  get type(): Type {
    return this._type;
  }

  set type(value: Type) {
    this._type = value;
    this.originalTypeDoc = this.type.typeDoc || null;
  }

  constructor(private typeService: TypesService, private snackBar: MatSnackBar) {
  }

  cancelEdits() {
    this.type.typeDoc = this.originalTypeDoc;
  }

  doSave(newContent: string) {
    console.log('Saving changes');
    // TODO :  Sanitize and escape any [[ or ]], as these are the code markers
    const typeDoc = `[[ ${newContent} ]]`;

    const namespaceDeclaration = (this.type.name.namespace) ? `namespace ${this.type.name.namespace}\n\n` : '';
    const taxi = `import ${this.type.name.fullyQualifiedName}\n\n${namespaceDeclaration}${typeDoc} \ntype extension ${this.type.name.name} {}`;
    console.log(taxi);
    this.loading = true;
    this.typeService.createExtensionSchemaFromTaxi(this.type.name, 'TypeDoc', taxi)
      .subscribe(result => {
        this.loading = false;
        this.snackBar.open('Changes saved', 'Dismiss', {duration: 3000});
      }, error => {
        console.log(error);
        this.loading = false;
        this.snackBar.open('Something went wrong.  Your changes have not been saved.', 'Dismiss', {duration: 3000});
      });
  }

}
