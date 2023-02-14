module.exports.taxiPrismLanguage = (Prism) => {
  Prism.languages.taxi = Prism.languages.extend('clike', {
    'keyword': [
      /\b(?:type|inherits|model|service|operation|query|given|as|alias)\b/,
      // keywords that have to be followed by an identifier
      /\b(?:type|model|query|service|operation|table|stream|find|stream)\b(?=\s*(?:[{_$a-zA-Z\xA0-\uFFFF]|$))/,
    ],
    'class-name': [
      {
        pattern: /(\b(?:type|inherits|model|service|operation|query|table|stream|as)\s+)/,
        lookbehind: true,
        greedy: true,
        inside: null // see below
      }, {
        // Matches types declared after a colon (ie., in a projection ...
        // as { foo : Bar }
        // We only match class names starting with a capital letter.
        // This is by convention, and lets our code look better when doing function calls like
        // concat(Foo, Bar)
        pattern: /\b(\w+)\s*:\s*([A-Z]\w+)\b/,
        lookbehind: true,
        greedy: true,
        inside: {
          'punctuation': /:/
        }
      },
      {
        // Matches class names in find { Foo } and stream { Foo }
        pattern: /\b(?:find|stream)\s*{\s*(\w*)\b/,
        inside: {
          'keyword': /find|stream/,
          'punctuation' : /{/,
        }
        // lookbehind: true
      }
    ],
    'builtin': /\b(?:Array|Stream|Any|String|Number|Date|LocalDate|Instant|Decimal|Double|Void)\b/,
  });

  // a version of typescript specifically for highlighting types
   const typeInside = Prism.languages.extend('taxi', {});
   delete typeInside['class-name'];

  Prism.languages.taxi['class-name'].inside = typeInside;

  Prism.languages.insertBefore('taxi', 'function', {
    'decorator': {
      pattern: /@[$\w\xA0-\uFFFF]+/,
      inside: {
        'at': {
          pattern: /^@/,
          alias: 'operator'
        },
        'function': /^[\s\S]+/
      }
    },
    'generic-function': {
      // e.g. foo<T extends "bar" | "baz">( ...
      pattern: /#?(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*\s*<(?:[^<>]|<(?:[^<>]|<[^<>]*>)*>)*>(?=\s*\()/,
      greedy: true,
      inside: {
        'function': /^#?(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*/,
        'generic': {
          pattern: /<[\s\S]+/, // everything after the first <
          alias: 'class-name',
          inside: typeInside
        }
      }
    }
  });


};
