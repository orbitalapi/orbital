import {findType} from "../../services/schema";
import {testSchema} from "../../object-view/test-schema";
import {expect} from '@jest/globals';
import {appendToQuery} from "./query-code-generator";

describe('generating a query', function () {
  const type = findType(testSchema, 'demo.Customer');
  const typeName = type.name;
  it('should append find {} to an empty query', () => {
    const result = appendToQuery('', typeName)
    expect(result).toBe(`import demo.Customer

find { Customer }`)
  })

  it('should append to existing projection', () => {
    const result = appendToQuery('find { Person } as {', typeName)
  })
});
