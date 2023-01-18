module.exports.taxiPrismLanguage = (Prism) => {
  Prism.languages.taxi = Prism.languages.extend('typescript', {
    'class-name': {
      pattern: /(\b(?:type|inherits|model|service|operation|query|table|stream)\s+)(?!keyof\b)(?!\s)[_$a-zA-Z\xA0-\uFFFF](?:(?!\s)[$\w\xA0-\uFFFF])*(?:\s*<(?:[^<>]|<(?:[^<>]|<[^<>]*>)*>)*>)?/,
      lookbehind: true,
      greedy: true,
      inside: null // see below
    },
    'builtin': /\b(?:Array|Stream|Any|String|Number|Date|LocalDate|Instant|Decimal|Double|Void)\b/,
  });

  // The keywords
  Prism.languages.taxi.keyword.push(
    /\b(?:type|inherits|model|service|operation|query|given|as|alias)\b/,
    // keywords that have to be followed by an identifier
    /\b(?:type|model|query|service|operation|table|stream)\b(?=\s*(?:[{_$a-zA-Z\xA0-\uFFFF]|$))/
  );

  // a version of typescript specifically for highlighting types
  var typeInside = Prism.languages.extend('taxi', {});
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
