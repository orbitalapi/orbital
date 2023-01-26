export interface CodeSnippet {
  name: string;
  lang: string;
  code: string;
}

export type CodeSnippetMap = { [index: string]: CodeSnippet }


export type HighlightedCodeSnippet = String

export type HighlightedCodeSnippetMap = { [index: string]: HighlightedCodeSnippet }
