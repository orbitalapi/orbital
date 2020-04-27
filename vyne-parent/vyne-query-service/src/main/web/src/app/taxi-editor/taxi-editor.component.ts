import {Component, OnInit} from '@angular/core';
import {MonacoEditorModel} from './monaco-editor.component';

@Component({
  selector: 'app-taxi-editor',
  templateUrl: './taxi-editor.component.html',
  styleUrls: ['./taxi-editor.component.scss']
})
export class TaxiEditorComponent implements OnInit {

  editorOptions = {theme: 'vs-dark', language: 'javascript'};
  code = 'function x() {\nconsole.log("Hello world!");\n}';

  model: MonacoEditorModel = {
    value: this.code,
    language: 'javascript',
  };

  constructor() {
  }

  ngOnInit() {
  }

}
