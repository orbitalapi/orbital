import {languages} from 'monaco-editor';
import IMonarchLanguage = languages.IMonarchLanguage;
import LanguageConfiguration = languages.LanguageConfiguration;

export const TAXI_LANGUAGE_ID = 'taxi';
// This config was based on the kotlin config
export const taxiLanguageConfiguration: LanguageConfiguration = {
  // the default separators except `@$`
  wordPattern: /(-?\d*\.\d\w*)|([^\`\~\!\#\%\^\&\*\(\)\-\=\+\[\{\]\}\\\|\;\:\'\"\,\.\<\>\/\?\s]+)/g,
  comments: {
    lineComment: '//',
    blockComment: ['/*', '*/'],
  },
  __electricCharacterSupport: {
    docComment: {
      open: '[[',
      close: ']]'
    }
  },
  brackets: [
    ['{', '}'],
    ['[', ']'],
    ['(', ')'],
  ],
  autoClosingPairs: [
    {open: '{', close: '}'},
    {open: '[', close: ']'},
    {open: '(', close: ')'},
    {open: '"', close: '"'},
    {open: '\'', close: '\''},
  ],
  surroundingPairs: [
    {open: '{', close: '}'},
    {open: '[', close: ']'},
    {open: '(', close: ')'},
    {open: '"', close: '"'},
    {open: '\'', close: '\''},
    {open: '<', close: '>'},
  ],
  folding: {
    markers: {
      start: new RegExp('^\\s*//\\s*(?:(?:#?region\\b)|(?:<editor-fold\\b))'),
      end: new RegExp('^\\s*//\\s*(?:(?:#?endregion\\b)|(?:</editor-fold>))')
    }
  }
};


export const taxiLanguageTokenProvider = <IMonarchLanguage>{
  defaultToken: '',
  tokenPostfix: '.taxi',

  keywords: [

    'type', 'type alias', 'inherits', 'enum', 'namespace', 'as', 'service', 'operation', 'import',
    'policy', 'against', 'read', 'write', 'case', 'else', 'permit', 'filter', 'closed',
    'true', 'false', 'null',
    'Boolean', 'String', 'Int', 'Decimal', 'Date', 'Time', 'DateTime', 'Instant', 'Any', 'Double', 'Void'
  ],

  operators: [
    '+', '-', '*', '/', '%', '=', '+=', '-=', '*=', '/=',
    '%=', '++', '--', '&&', '||', '!', '==', '!=', '===',
    '!==', '>', '<', '<=', '>=', '[', ']', '!!', '?.', '?:',
    '::', '..', ':', '?', '->', '@', ';', '$', '_'
  ],

  // we include these common regular expressions
  symbols: /[=><!~?:&|+\-*\/\^%]+/,
  escapes: /\\(?:[abfnrtv\\"']|x[0-9A-Fa-f]{1,4}|u[0-9A-Fa-f]{4}|U[0-9A-Fa-f]{8})/,
  digits: /\d+(_+\d+)*/,
  octaldigits: /[0-7]+(_+[0-7]+)*/,
  binarydigits: /[0-1]+(_+[0-1]+)*/,
  hexdigits: /[[0-9a-fA-F]+(_+[0-9a-fA-F]+)*/,

  // The main tokenizer for our languages
  tokenizer: {

    root: [
      // class name highlighting
      [/[A-Z][\w\$]*/, 'type.identifier'],

      // identifiers and keywords
      [/[a-zA-Z_$][\w$]*/, {
        cases: {
          '@keywords': {token: 'keyword.$0'},
          '@default': 'identifier'
        }
      }],

      // whitespace
      {include: '@whitespace'},

      // delimiters and operators
      [/[{}()\[\]]/, '@brackets'],
      [/[<>](?!@symbols)/, '@brackets'],
      [/@symbols/, {
        cases: {
          '@operators': 'delimiter',
          '@default': ''
        }
      }],

      // @ annotations.
      [/@\s*[a-zA-Z_\$][\w\$]*/, 'annotation'],

      // numbers
      [/(@digits)[eE]([\-+]?(@digits))?[fFdD]?/, 'number.float'],
      [/(@digits)\.(@digits)([eE][\-+]?(@digits))?[fFdD]?/, 'number.float'],
      [/0[xX](@hexdigits)[Ll]?/, 'number.hex'],
      [/0(@octaldigits)[Ll]?/, 'number.octal'],
      [/0[bB](@binarydigits)[Ll]?/, 'number.binary'],
      [/(@digits)[fFdD]/, 'number.float'],
      [/(@digits)[lL]?/, 'number'],

      // delimiter: after number because of .\d floats
      [/[;,.]/, 'delimiter'],

      // strings
      [/"([^"\\]|\\.)*$/, 'string.invalid'],  // non-teminated string
      [/"""/, 'string', '@multistring'],
      [/"/, 'string', '@string'],

      // characters
      [/'[^\\']'/, 'string'],
      [/(')(@escapes)(')/, ['string', 'string.escape', 'string']],
      [/'/, 'string.invalid']
    ],

    whitespace: [
      [/[ \t\r\n]+/, ''],
      [/\/\*\*(?!\/)/, 'comment.doc', '@javadoc'],
      [/\/\*/, 'comment', '@comment'],
      [/\/\/.*$/, 'comment'],
    ],

    comment: [
      [/[^\/*]+/, 'comment'],
      [/\/\*/, 'comment', '@comment'],
      [/\*\//, 'comment', '@pop'],
      [/[\/*]/, 'comment']
    ],
    // Identical copy of comment above, except for the addition of .doc
    javadoc: [
      [/[^\/*]+/, 'comment.doc'],
      [/\/\*/, 'comment.doc', '@push'],
      [/\/\*/, 'comment.doc.invalid'],
      [/\*\//, 'comment.doc', '@pop'],
      [/[\/*]/, 'comment.doc']
    ],

    string: [
      [/[^\\"]+/, 'string'],
      [/@escapes/, 'string.escape'],
      [/\\./, 'string.escape.invalid'],
      [/"/, 'string', '@pop']
    ],

    multistring: [
      [/[^\\"]+/, 'string'],
      [/@escapes/, 'string.escape'],
      [/\\./, 'string.escape.invalid'],
      [/"""/, 'string', '@pop'],
      [/./, 'string']
    ],
  },
};
