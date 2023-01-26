import { highlightCode } from '../../../remark/utils'
import { CodeSnippetMap, HighlightedCodeSnippetMap } from '@/components/Guides/CodeSnippet';


export function highlightCodeSnippets(codeSnippets: CodeSnippetMap): HighlightedCodeSnippetMap {
  function highlight(code) {
    return code.lang && code.lang === 'terminal' ? code.code : highlightCode(code.code, code.lang)
  }

  const highlighted = {};
  Object.keys(codeSnippets).map(key => {
    const snippet = codeSnippets[key];
    highlighted[key] = highlight(snippet);
  })
  return highlighted;
}
