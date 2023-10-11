import * as monaco from 'monaco-editor/esm/vs/editor/editor.api';

export class JsonTypeInlayHintProvider implements monaco.languages.InlayHintsProvider {
  public static readonly hints: Map<monaco.Uri, monaco.languages.InlayHint[]> = new Map()
  provideInlayHints(model: monaco.editor.ITextModel, range: monaco.Range, token: monaco.CancellationToken): monaco.languages.ProviderResult<monaco.languages.InlayHintList> {
    const registeredHints = JsonTypeInlayHintProvider.hints.get(model.uri)
    return {
      hints: registeredHints,
      dispose: () => {
      },
    };
  }
}
