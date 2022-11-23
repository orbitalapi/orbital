import {Clipboard} from '@angular/cdk/clipboard';

export type CopyQueryFormat = 'query' | 'curl';
export function copyQueryAs(query: string, queryEndpoint: string, format: CopyQueryFormat, clipboard: Clipboard) {

  function wrapTextInCurl(query: string) {
    return `curl -X POST '${queryEndpoint}' \\
  -H 'Content-Type: application/taxiql' \\
  --data-raw '${query}'`
  }

  if (format === 'query') {
    clipboard.copy(query);
  } else if (format === 'curl') {
    const curlStatement = wrapTextInCurl(query)
    clipboard.copy(curlStatement);
  }
}
