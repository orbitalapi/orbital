import * as monaco from 'monaco-editor/esm/vs/editor/editor.api';
import {Subject} from "rxjs";

class HintMap {
  private readonly hints: Map<monaco.Uri, monaco.languages.InlayHint[]> = new Map()
  hintsUpdated = new Subject<void>()
  registerHints(uri: monaco.Uri, hints: monaco.languages.InlayHint[]) {
    this.hints.set(uri, hints);
    this.hintsUpdated.next();
  }
  getHints(uri: monaco.Uri):monaco.languages.InlayHint[] {
    return this.hints.get(uri);
  }
}

export class JsonTypeInlayHintProvider implements monaco.languages.InlayHintsProvider {

  public static readonly hintMap = new HintMap();
  private _onDidChange = new monaco.Emitter<void>()

  constructor() {
    JsonTypeInlayHintProvider.hintMap.hintsUpdated.subscribe(next => {
      this._onDidChange.fire();
    })
  }

  onDidChangeInlayHints:monaco.IEvent<void> = this._onDidChange.event;

  provideInlayHints(model: monaco.editor.ITextModel, range: monaco.Range, token: monaco.CancellationToken): monaco.languages.ProviderResult<monaco.languages.InlayHintList> {
    const registeredHints = JsonTypeInlayHintProvider.hintMap.getHints(model.uri)
    return {
      hints: registeredHints,
      dispose: () => {
      },
    };
  }
}
