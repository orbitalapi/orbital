export function taxiLangDef(hljs) {
  const JS_IDENT_RE = '[A-Za-z$_][0-9A-Za-z$_]*';
  const DECORATOR = {
    className: 'meta',
    begin: '@' + JS_IDENT_RE,
  };


  const KEYWORD = {
    keyword: 'type alias enum namespace as service operation',
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
