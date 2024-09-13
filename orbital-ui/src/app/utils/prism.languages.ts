// A spot for any custom Prism language syntax schemas

import Prism from 'prismjs'

export const taxi = Prism.languages.extend('clike', {
  // Keywords
  'keyword': /\b(?:type|type alias|find|query|stream|inherits|enum|namespace|as|service|operation|import|policy|against|read|write|case|else|permit|filter|closed|true|false|null|Boolean|String|Int|Decimal|Date|Time|DateTime|Instant|Any|Double|Void)\b/,

  // Operators
  'operator': /[+\-*/%=!<>&|^~?:]+|::|\.\.\.|[@;_$]/,

  // Annotations
  'annotation': /@\s*[a-zA-Z_$][\w$]*/,

  // Numbers
  'number': /\b\d+(_\d+)*(\.\d+(_\d+)*)?([eE][\-+]?\d+(_\d+)*)?[fFdD]?|0[xX][\da-fA-F]+[Ll]?|0[bB][01]+[Ll]?|0[oO]?[0-7]+[Ll]?/,

  // Punctuation
  'punctuation': /[{}[\];(),.:]/,

  // Strings
  'string': [
    {
      pattern: /"""/, // Multiline string
      greedy: true,
      inside: {
        'string': /[\s\S]*?"""/
      }
    },
    {
      pattern: /"(?:\\.|[^\\"])*"/, // Single-line string
      greedy: true
    },
    {
      pattern: /'[^\\']'/, // Single character
      greedy: true
    }
  ],

  // Comments
  'comment': [
    {
      pattern: /\/\*\*[\s\S]*?\*\//, // Javadoc-style comments
      greedy: true
    },
    {
      pattern: /\[\[[\s\S]*?\]\]/, // TaxiDoc comments
      greedy: true
    },
    {
      pattern: /\/\/.*/, // Line comments
      greedy: true
    },
    {
      pattern: /\/\*[\s\S]*?\*\//, // Block comments
      greedy: true
    }
  ],

  // Types and Identifiers
  'type': {
    pattern: /\b[A-Z][\w$]*\b/,
    alias: 'class-name'
  },

  // Escapes inside strings
  'string-escape': {
    pattern: /\\(?:[abfnrtv\\"']|x[\da-fA-F]{1,4}|u[\da-fA-F]{4}|U[\da-fA-F]{8})/,
    alias: 'escape'
  }
});


