// Difficulty: "Easy"
// Language definition for Java
function monarchDef() {
   return {
       // Set defaultToken to invalid to see what you do not tokenize yet
       // defaultToken: 'invalid',

       keywords: [
           'namespace', 'service', 'operation', 'type', 'alias', 'as'
       ],

       typeKeywords: [
           'Boolean',
           'String',
           'Int',
           'Double',
           'Decimal',
           //    The "full-date" notation of RFC3339, namely yyyy-mm-dd. Does not support time or time zone-offset notation.
           'Date',
           //    The "partial-time" notation of RFC3339, namely hh:mm:ss[.ff...]. Does not support date or time zone-offset notation.
           'Time',
           // Combined date-only and time-only with a separator of "T", namely yyyy-mm-ddThh:mm:ss[.ff...]. Does not support a time zone offset.
           'DateTime',
           // A timestamp, indicating an absolute point in time.  Includes timestamp.  Should be rfc3339 format.  (eg: 2016-02-28T16:41:41.090Z)
           'Instant',
       ],

       operators: [
           '=', '>', '<', '!', '~', '?', ':',
           '==', '<=', '>=', '!=', '&&', '||', '++', '--',
           '+', '-', '*', '/', '&', '|', '^', '%', '<<',
           '>>', '>>>', '+=', '-=', '*=', '/=', '&=', '|=',
           '^=', '%=', '<<=', '>>=', '>>>='
       ],

       // we include these common regular expressions
       symbols: /[=><!~?:&|+\-*\/\^%]+/,
       escapes: /\\(?:[abfnrtv\\"']|x[0-9A-Fa-f]{1,4}|u[0-9A-Fa-f]{4}|U[0-9A-Fa-f]{8})/,

   // The main tokenizer for our languages
       tokenizer: {
           root: [
   // identifiers and keywords
               [/[a-z_$][\w$]*/, {
                   cases: {
                       '@typeKeywords': 'keyword',
                       '@keywords': 'keyword',
                       '@default': 'identifier'
                   }
               }],
               [/[A-Z][\w\$]*/, 'type.identifier'],  // to show class names nicely

   // whitespace
               {include: '@whitespace'},

   // delimiters and operators
               [/[{}()\[\]]/, '@brackets'],
               [/[<>](?!@symbols)/, '@brackets'],
               [/@symbols/, {
                   cases: {
                       '@operators': 'operator',
                       '@default': ''
                   }
               }],

   // @ annotations.
   // As an example, we emit a debugging log message on these tokens.
   // Note: message are supressed during the first load -- change some lines to see them.
               [/@\s*[a-zA-Z_\$][\w\$]*/, {token: 'annotation', log: 'annotation token: $0'}],

   // numbers
               [/\d*\.\d+([eE][\-+]?\d+)?[fFdD]?/, 'number.float'],
               [/0[xX][0-9a-fA-F_]*[0-9a-fA-F][Ll]?/, 'number.hex'],
               [/0[0-7_]*[0-7][Ll]?/, 'number.octal'],
               [/0[bB][0-1_]*[0-1][Ll]?/, 'number.binary'],
               [/\d+[lL]?/, 'number'],

   // delimiter: after number because of .\d floats
               [/[;,.]/, 'delimiter'],

   // strings
               [/"([^"\\]|\\.)*$/, 'string.invalid'],  // non-teminated string
               [/"/, 'string', '@string'],

   // characters
               [/'[^\\']'/, 'string'],
               [/(')(@escapes)(')/, ['string', 'string.escape', 'string']],
               [/'/, 'string.invalid']
           ],

           whitespace: [
               [/[ \t\r\n]+/, 'white'],
               [/\/\*/, 'comment', '@comment'],
               [/\/\/.*$/, 'comment'],
           ],

           comment: [
               [/[^\/*]+/, 'comment'],
               // [/\/\*/, 'comment', '@push' ],    // nested comment not allowed :-(
               [/\/\*/, 'comment.invalid'],
               ["\\*/", 'comment', '@pop'],
               [/[\/*]/, 'comment']
           ],

           string: [
               [/[^\\"]+/, 'string'],
               [/@escapes/, 'string.escape'],
               [/\\./, 'string.escape.invalid'],
               [/"/, 'string', '@pop']
           ],
       },
   };
   }
