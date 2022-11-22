import {Component, EventEmitter, Input, Output} from '@angular/core';
import {NamedAndDocumented, Type} from '../../services/schema';
import {TypesService} from '../../services/types.service';
import {MatSnackBar} from '@angular/material/snack-bar';
import {CommitMode} from '../type-viewer.component';
import {debounceTime} from 'rxjs/operators';

@Component({
  selector: 'app-description-editor-container',
  template: `
    <app-description-editor [documentationSource]="type"
                            (save)="doSave($event)"
                            [editable]="editable"
                            [showControlBar]="commitMode === 'immediate'"
                            (valueChanged)="typeDocChangeHandler.next($event)"
                            [placeholder]="'Write something great that describes the type ' + type.name.name"></app-description-editor>
  `,
  styleUrls: ['./description-editor.component.scss']
})
export class DescriptionEditorContainerComponent {

  originalTypeDoc: string | null;
  loading = false;

  @Input()
  editable = false;

  @Input()
  commitMode: CommitMode = 'immediate';

  private _type: NamedAndDocumented;

  @Input()
  get type(): NamedAndDocumented {
    return this._type;
  }

  /**
   * Emitted when the type has been updated, but not committed to the back-end.
   * (ie., when then commitMode = 'explicit')
   */
  @Output()
  updateDeferred = new EventEmitter<NamedAndDocumented>();

  set type(value: NamedAndDocumented) {
    this._type = value;
    this.originalTypeDoc = this.type.typeDoc || null;
  }

  typeDocChangeHandler: EventEmitter<string>;

  constructor(private typeService: TypesService, private snackBar: MatSnackBar) {
    this.typeDocChangeHandler = new EventEmitter<string>();
    this.typeDocChangeHandler
      .pipe(
        debounceTime(350) // We're debouncing - ie., dispatching once after typing has paused for x ms.
      )
      .subscribe(value => {
          // If we're not writing automatically to the server, then update the typeDoc
          // on the type directly, to allow saving later.
          // The serialization is expensive, so do this periodically, rather than on every keystroke.
          if (this.commitMode === 'explicit') {
            this.type.typeDoc = value;
            this.updateDeferred.emit(this.type);
            console.log(`Typedoc on type ${this.type.name.fullyQualifiedName} updated`);
          }
        }
      );
  }

  cancelEdits() {
    this.type.typeDoc = this.originalTypeDoc;
  }

  doSave(newContent: string) {
    if (this.commitMode === 'immediate') {
      this.commitUpdatedDocs(newContent);
    } else {
      console.log('TypeDoc updated, deferring commit');
      this.type.typeDoc = newContent;
      this.updateDeferred.emit(this.type);
    }

  }

  private commitUpdatedDocs(newContent: string) {
    console.log('Saving changes');
    // TODO :  Sanitize and escape any [[ or ]], as these are the code markers
    const typeDoc = `[[ ${newContent} ]]`;

    const namespaceDeclaration = (this.type.name.namespace) ? `namespace ${this.type.name.namespace}\n\n` : '';
    // eslint-disable-next-line max-len
    const taxi = `import ${this.type.name.fullyQualifiedName}\n\n${namespaceDeclaration}${typeDoc} \ntype extension ${this.type.name.name} {}`;
    this.loading = true;
    this.typeService.addChangesToChangeset(this.type.name, 'TypeDoc', taxi)
      .subscribe(() => {
        this.loading = false;
        this.snackBar.open('Draft saved', 'Dismiss', {duration: 3000});
      }, error => {
        console.log(error);
        this.loading = false;
        this.snackBar.open('Something went wrong.  Your changes have not been saved.', 'Dismiss', {duration: 3000});
      });
  }
}
