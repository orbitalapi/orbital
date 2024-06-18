import * as monaco from 'monaco-editor/esm/vs/editor/editor.api';
import {TypePosition} from "../model-designer/taxi-parser.service";
import {languages} from 'monaco-editor';
import InlayHint = languages.InlayHint;
import InlayHintKind = languages.InlayHintKind;

export class JsonTypeInlayHintProvider implements monaco.languages.InlayHintsProvider {

  private _onDidChange = new monaco.Emitter<void>()

  private inlayHints = new Map<string,InlayHint[]>();
  private counter = 0;

  constructor() {
  }

  onDidChangeInlayHints:monaco.IEvent<void> = this._onDidChange.event;

  provideInlayHints(model: monaco.editor.ITextModel, range: monaco.Range, token: monaco.CancellationToken): monaco.languages.ProviderResult<monaco.languages.InlayHintList> {
    const hints = this.inlayHints.get(model.uri.toString()) || [];
    return {
      hints,
      dispose: () => {
      },
    };
  }

  setHints(modelUri: string, value: TypePosition[]) {
    this.counter++;
    const hints = value.map(hint => {
      return {
        kind: InlayHintKind.Type,
        label: hint.type.shortDisplayName,
        position: {
          column: hint.start.char,
          lineNumber: hint.start.line
        },
        tooltip: hint.type.longDisplayName,
        paddingLeft: true
      }
    })
    this.inlayHints.set(modelUri, hints);
    this._onDidChange.fire()
  }
}
