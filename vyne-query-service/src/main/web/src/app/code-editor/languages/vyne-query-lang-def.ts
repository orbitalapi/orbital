export function vyneQueryLanguageDef(hljs) {
  const JS_IDENT_RE = '[A-Za-z$_][0-9A-Za-z$_]*';
  const DECORATOR = {
    className: 'meta',
    begin: '@' + JS_IDENT_RE,
  };


  const POLICY_KEYWORDS = 'policy against read write case else permit filter';
  const MAPPING_KEYWORDS = 'by column xpath json';
  const KEYWORD = {
    keyword: `type alias inherits enum namespace as service operation import ${POLICY_KEYWORDS} ${MAPPING_KEYWORDS}`,
    literal: 'true false null',
    built_in: 'Boolean String Int Decimal Date Time DateTime Instant Array Any Double Void'
  };
  return {
    case_insensitive: false,
    keywords: KEYWORD,
    contains: [
      hljs.APOS_STRING_MODE,
      hljs.QUOTE_STRING_MODE,
      hljs.C_LINE_COMMENT_MODE,
      hljs.C_BLOCK_COMMENT_MODE,
      DECORATOR
    ]
  };
}
