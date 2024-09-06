import {Component, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {ControlContainer, FormControl, FormsModule, NgControl, NgModelGroup, ReactiveFormsModule} from '@angular/forms';
import {Observable, of, Subject, switchMap} from 'rxjs';
import {CommonModule} from '@angular/common';
import {TuiErrorModule} from '@taiga-ui/core';
import {TuiFileLike, TuiInputFilesModule, TuiInputModule} from '@taiga-ui/kit';
import {FileExtensionValidatorDirective} from './file-extension-validator.directive';

// A component that combines the Tui components required for file upload and a path based input field.
// It also handles the validation that's required for the path based flow.
@Component({
  selector: 'app-file-path-or-upload',
  viewProviders: [{provide: ControlContainer, useExisting: NgModelGroup}],
  standalone: true,
  imports: [
    TuiInputFilesModule,
    ReactiveFormsModule,
    CommonModule,
    TuiInputModule,
    FormsModule,
    FileExtensionValidatorDirective,
    TuiErrorModule
  ],
  template: `
    @if (mode === 'upload' && editable) {
      <tui-input-files
        *ngIf="!fileDropControl.value"
        [accept]="filesAccepted.join(',')"
        [link]="uploadLabel"
        [formControl]="fileDropControl"
        (reject)="onReject($event)"
      ></tui-input-files>
      <tui-files class="tui-space_top-1">
        <tui-file
          *ngIf="loadedFiles$ | async as file"
          [file]="file"
          [showDelete]="fileDropControl.enabled"
          (removed)="removeFile()"
        ></tui-file>
        <tui-file
          *ngIf="rejectedFiles$ | async as file"
          state="error"
          [file]="file"
          [showDelete]="fileDropControl.enabled"
          (removed)="clearRejected()"
        ></tui-file>
      </tui-files>
    } @else {
      <tui-input
        [ngModel]="path"
        (ngModelChange)="onPathChanged($event)"
        appFileExtensionValidator [appFileExtensionValidator]="filesAccepted"
        required
        name="path"
        [readOnly]="!editable"
        #pathName="ngModel"
      >
        Path
        <span class="tui-required"></span>
      </tui-input>
      <tui-error
        [error]="pathName$?.invalid && (pathName$.dirty || pathName$.touched) ? fileExtensionErrorLabel : null"
      >
      </tui-error>
    }
  `,
})
export class FilePathOrUploadComponent {
  @Input()
  editable: boolean = true;

  @Input()
  path: string;

  @Input()
  mode: 'path' | 'upload';

  @Input()
  uploadLabel: string;

  @Input()
  filesAccepted: string[];

  @Input()
  fileExtensionErrorLabel: string;

  @Output()
  pathChanged = new EventEmitter<string>();

  @Output()
  fileChanged = new EventEmitter<string>()

  @ViewChild('pathName')
  pathName$: NgControl

  errorMessage: string;

  readonly fileDropControl = new FormControl<TuiFileLike | null>(null);

  readonly rejectedFiles$ = new Subject<TuiFileLike | null>();
  readonly loadedFiles$ = this.fileDropControl.valueChanges.pipe(
    switchMap(file => (file ? this.makeRequest(file) : of(null))),
  );

  onPathChanged(value: string) {
    this.path = value;
    this.pathChanged.emit(value);
  }

  onReject(file: TuiFileLike | readonly TuiFileLike[]): void {
    this.rejectedFiles$.next(file as TuiFileLike);
  }

  makeRequest(file: TuiFileLike): Observable<TuiFileLike | null> {
    this.rejectedFiles$.next(null);
    const fileReader = new FileReader();
    fileReader.onloadend = () => {
      this.fileChanged.emit(fileReader.result as string)
    }
    fileReader.readAsText(file as File);
    return of(file)
  }

  removeFile(): void {
    this.fileDropControl.setValue(null);
  }

  clearRejected(): void {
    this.removeFile();
    this.rejectedFiles$.next(null);
  }
}
