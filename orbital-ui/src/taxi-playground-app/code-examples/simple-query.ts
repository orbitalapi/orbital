import {StubQueryMessageWithSlug} from "../../app/services/query.service";

export const simpleQuery:StubQueryMessageWithSlug = {
  title: 'Hello, world query',
  slug: 'hello-world-query',
  query: {
    "layout": {
      "showSchema": false,
      "showDiagram": false,
      "showReadme": false,
      "showQuery": true
    },
    schema: '',
    query: 'find { 1 + 2 }',
    stubs: [],
    parameters: {},
    readme: `Here's a snippet:
\`\`\`taxiql
find { 2 + 3 }
\`\`\`
    `
  }
}
