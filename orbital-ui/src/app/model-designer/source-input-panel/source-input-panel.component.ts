import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  Output,
  ViewChild
} from '@angular/core';
import {UntypedFormControl} from "@angular/forms";
import {TuiFileLike} from "@taiga-ui/kit";
import * as monaco from "monaco-editor";
import {editor} from "monaco-editor";
import {Subject} from 'rxjs';
import {debounceTime} from "rxjs/operators";
import ITextModel = editor.ITextModel;
import IStandaloneCodeEditor = editor.IStandaloneCodeEditor;
import IModelContentChangedEvent = editor.IModelContentChangedEvent;

@Component({
    selector: 'app-designer-source-input-panel',
    template: `
    <app-panel-header title="Source content" [isSecondary]="true">
      <div class="spacer"></div>
      <button tuiButton size="s" appearance="outline-grayscale" *ngIf="editorVisible" (click)="clearContent()">Clear</button>
    </app-panel-header>
    <div class="empty-state" *ngIf="!editorVisible">
      <tui-notification class="onboarding-text" appearance="neutral" size="s">
        Provide a data sample to design a Taxi schema for (you can upload CSV, TSV, PSV, JSON or XML files)
      </tui-notification>
      <label
        *ngIf="!fileDropControl.value"
        tuiInputFiles
      >
        <input
          [formControl]="fileDropControl"
          tuiInputFiles
          (reject)="onReject($event)"
          [maxFileSize]="8*1024*1024"
          accept=".csv, .tsv, .psv, .json, .xml"
        />
      </label>
      <tui-files *ngIf="rejectedFiles$ | async as file">
        <tui-file
          state="error"
          [file]="file"
          [showDelete]="fileDropControl.enabled"
          (remove)="clearRejected()"
        ></tui-file>
      </tui-files>
      <tui-files *ngIf="loadingFiles$ | async as file">
        <tui-file
          state="loading"
          [file]="file"
          [showDelete]="fileDropControl.enabled"
        ></tui-file>
      </tui-files>
      <div>OR</div>
      <button tuiButton appearance="outline-grayscale" (click)="showEditor()">Start by writing</button>
    </div>
    <div class="editor-container" #codeEditorContainer *ngIf="editorVisible">

    </div>
  `,
    styleUrls: ['./source-input-panel.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class SourceInputPanelComponent {

    readonly fileDropControl = new UntypedFormControl();
    readonly rejectedFiles$ = new Subject<TuiFileLike | null>();
    readonly loadingFiles$ = new Subject<TuiFileLike | null>();
    editorVisible = false;

    private monacoEditor: IStandaloneCodeEditor;
    private monacoModel: ITextModel;

    private _content: string;
    private monacoModelChanged$ = new EventEmitter<IModelContentChangedEvent>();

    @Input()
    get content(): string {
        return this._content;
    }

    set content(value: string) {
        this.updateContent(value)
    }

    private updateContent(value: string, alsoUpdateMonaco: boolean = true) {
        if (this._content === value) {
            return;
        }
        this._content = value;
        if (this.monacoModel && alsoUpdateMonaco) {
            this.monacoModel.setValue(value);
        }
        this.contentChange.emit(value);
        if (this.content !== null && this.content !== undefined) {
            this.editorVisible = true;
        }
        this.changeDetector.markForCheck();
    }

    @Output()
    contentChange = new EventEmitter<string>();

    @Output()
    contentCleared = new EventEmitter<void>();

    constructor(private changeDetector: ChangeDetectorRef) {
        this.monacoModelChanged$.pipe(
            debounceTime(250),
        ).subscribe(e => {
            this.updateContent(this.monacoModel.getValue(), false);
        });


        this.fileDropControl.valueChanges.subscribe(next => {
            if (next !== null && next !== undefined) {
                this.loadFileContent(next)
            }
        })
    }

    private loadFileContent(fileLike: TuiFileLike) {
        this.rejectedFiles$.next(null);
        this.loadingFiles$.next(fileLike);
        const fileReader = new FileReader();
        fileReader.onloadend = () => {
            this.updateContent(fileReader.result as string, true);
            this.loadingFiles$.next(null);
        }
        fileReader.readAsText(fileLike as File);
    }

    onReject(file: TuiFileLike | TuiFileLike[]): void {
      this.rejectedFiles$.next(file as TuiFileLike);
    }

    clearRejected(): void {
      this.removeFile();
      this.rejectedFiles$.next(null);
    }

    private removeFile(): void {
      this.fileDropControl.setValue(null);
    }

    showEditor() {
        this.editorVisible = true;
        this.changeDetector.markForCheck();
    }

    private _codeEditorContainer: ElementRef;
    @ViewChild('codeEditorContainer', {static: false})
    get codeEditorContainer(): ElementRef {
        return this._codeEditorContainer;
    }

    set codeEditorContainer(value: ElementRef) {
        this._codeEditorContainer = value;
        if (value === null || value === undefined) {
            this.destroyMonacoIfExists();
        } else {
            this.createMonacoEditor();
        }
    }

    private destroyMonacoIfExists() {
        if (this.monacoEditor) {
            this.monacoEditor.dispose();
        }
    }

    private createMonacoEditor(): void {
        this.destroyMonacoIfExists();
        this.monacoModel = monaco.editor.createModel(this.content);
        this.monacoModel.onDidChangeContent(e => this.monacoModelChanged$.next(e));
        this.monacoEditor = monaco.editor.create(this._codeEditorContainer.nativeElement, {
            model: this.monacoModel,
            glyphMargin: true,
            lightbulb: {
                enabled: true
            },
            automaticLayout: true,
        });
    }

    clearContent() {
        this.fileDropControl.setValue(null);
        this.updateContent("", true);
        this.editorVisible = false;
        this.contentCleared.emit();
        this.changeDetector.markForCheck();
    }
}
