export function taxiLangDef(hljs) {
  const JS_IDENT_RE = '[A-Za-z$_][0-9A-Za-z$_]*';
  const DECORATOR = {
    className: 'meta', begin: '@[A-Za-z]+'
  };

  const BLOCK_COMMENT = hljs.COMMENT('/\\*\\*',
    '\\*/');

  const DOCS = hljs.COMMENT('\\[\\[', '\\]\\]');

  const POLICY_KEYWORDS = 'policy against read write case else permit filter';
  const MAPPING_KEYWORDS = 'by column xpath json';
  const KEYWORDS = {
    keyword: `type alias inherits enum namespace as service operation import ${POLICY_KEYWORDS} ${MAPPING_KEYWORDS}`,
    literal: 'true false null',
    built_in: 'Boolean String Int Decimal Date Time DateTime Instant Array Any Double Void'
  };
  const ARGS = {
    begin: '\\(',
    end: /\)/,
    keywords: KEYWORDS,
    contains: [
      'self',
      hljs.QUOTE_STRING_MODE,
      hljs.APOS_STRING_MODE,
      hljs.NUMBER_MODE
    ]
  };
  const PARAMS = {
    className: 'params',
    begin: /\(/, end: /\)/,
    excludeBegin: true,
    excludeEnd: true,
    keywords: KEYWORDS,
    contains: [
      hljs.C_LINE_COMMENT_MODE,
      hljs.C_BLOCK_COMMENT_MODE,
      DECORATOR,
      ARGS
    ]
  };

  return {
    case_insensitive: false,
    keywords: KEYWORDS,
    contains: [
      hljs.APOS_STRING_MODE,
      hljs.QUOTE_STRING_MODE,
      hljs.C_LINE_COMMENT_MODE,
      hljs.C_BLOCK_COMMENT_MODE,
      BLOCK_COMMENT,
      DECORATOR,
      PARAMS,
      DOCS,
      {
        className: 'class',
        beginKeywords: 'type model', end: /[{;=]/, excludeEnd: true,
        keywords: 'type model',
        illegal: /[:"\[\]]/,
        contains: [
          {beginKeywords: 'extends implements'},
          hljs.UNDERSCORE_TITLE_MODE
        ]
      },
    ]
  };
}
