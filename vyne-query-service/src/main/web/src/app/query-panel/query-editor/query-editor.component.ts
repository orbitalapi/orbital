import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {MonacoEditorLoaderService} from '@materia-ui/ngx-monaco-editor';
import {filter, take} from 'rxjs/operators';

import {editor} from 'monaco-editor';
import ITextModel = editor.ITextModel;
import ICodeEditor = editor.ICodeEditor;
import {QueryService, QueryResult, VyneQlQueryHistoryRecord, QueryHistoryRecord} from 'src/app/services/query.service';
import {QueryFailure} from '../query-wizard/query-wizard.component';
import {HttpErrorResponse} from '@angular/common/http';
import {vyneQueryLanguageConfiguration, vyneQueryLanguageTokenProvider} from './vyne-query-language.monaco';
import {Input} from '@angular/core';


declare const monaco: any; // monaco

@Component({
  selector: 'query-editor',
  templateUrl: './query-editor.component.html',
  styleUrls: ['./query-editor.component.scss']
})
export class QueryEditorComponent implements OnInit {

  @Input()
  initialQuery: QueryHistoryRecord;

  editorOptions: { theme: 'vs-dark', language: 'vyneQL' };
  monacoEditor: ICodeEditor;
  monacoModel: ITextModel;
  query: string;
  lastQueryResult: QueryResult | QueryFailure;
  loading = false;
  @Output()
  queryResultUpdated = new EventEmitter<QueryResult | QueryFailure>();
  @Output()
  loadingChanged = new EventEmitter<boolean>();

  constructor(private monacoLoaderService: MonacoEditorLoaderService, private queryService: QueryService) {
    this.monacoLoaderService.isMonacoLoaded$.pipe(
      filter(isLoaded => isLoaded),
      take(1),
    ).subscribe(() => {
      monaco.editor.onDidCreateEditor(editorInstance => {
        editorInstance.updateOptions({readOnly: false, minimap: {enabled: false}});
        this.monacoEditor = editorInstance;
        this.remeasure();
      });
      monaco.editor.onDidCreateModel(model => {
        this.monacoModel = model;
        monaco.editor.defineTheme('vyne', {
          base: 'vs-dark',
          inherit: true,
          rules: [
            {token: '', background: '#333f54'},
          ],
          colors: {
            ['editorBackground']: '#333f54',
          }
        });
        monaco.editor.setTheme('vyne');
        monaco.editor.setModelLanguage(model, 'vyneQL');
      });
      monaco.languages.register({id: 'vyneQL'});
      monaco.languages.setLanguageConfiguration('vyneQL', vyneQueryLanguageConfiguration);
      monaco.languages.setMonarchTokensProvider('vyneQL', vyneQueryLanguageTokenProvider);
      // here, we retrieve monaco-editor instance

    });
  }

  ngOnInit(): void {
    this.query = this.initialQuery ? (this.initialQuery as VyneQlQueryHistoryRecord).query : '';
  }

  remeasure() {
    setTimeout(() => {
      if (!this.monacoEditor) {
        return;
      }
      const editorDomNode = this.monacoEditor.getDomNode();
      const offsetHeightFixer = 20;
      if (editorDomNode) {
        const codeContainer = this.monacoEditor.getDomNode().getElementsByClassName('view-lines')[0] as HTMLElement;
        const calculatedHeight = codeContainer.offsetHeight + offsetHeightFixer + 'px';
        editorDomNode.style.height = calculatedHeight;
        const firstParent = editorDomNode.parentElement;
        firstParent.style.height = calculatedHeight;
        const secondParent = firstParent.parentElement;
        secondParent.style.height = calculatedHeight;
        console.log('Resizing Monaco editor to ' + calculatedHeight);
        this.monacoEditor.layout();
      }
    }, 10);
  }

  submitQuery() {
    this.lastQueryResult = null;
    this.loading = true;
    this.loadingChanged.emit(true);

    this.queryService.submitVyneQlQuery(this.query).subscribe(
      result => {
        this.loading = false;
        this.lastQueryResult = result;
        this.queryResultUpdated.emit(this.lastQueryResult);
        this.loadingChanged.emit(false);
      },
      error => {
        this.loading = false;
        const errorResponse = error as HttpErrorResponse;
        if (errorResponse.error && (errorResponse.error as any).hasOwnProperty('profilerOperation')) {
          this.lastQueryResult = new QueryFailure(
            errorResponse.error.message,
            errorResponse.error.profilerOperation,
            errorResponse.error.remoteCalls);
          this.queryResultUpdated.emit(this.lastQueryResult);
          this.loadingChanged.emit(false);
        } else {
          // There was an unhandled error...
          console.error('An unhandled error occurred:');
          console.error(JSON.stringify(error));

        }
      }
    );
  }
}
