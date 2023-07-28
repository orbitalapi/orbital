import {Snippet} from "./query-snippet-panel.component";
import {PrimitiveTypeNames} from "../services/taxi";
import {CodeGenRequest} from "./query-snippet-container.component";
import {findType, Schema, Type} from "../services/schema";
import {isNullOrUndefined} from "util";

export function kotlinGenerator(request: CodeGenRequest): Snippet[] {
  const dependencySnippet = new Snippet(
    'Dependencies',
    'xml',
    `<dependency>
   <groupId>com.orbitalhq</groupId>
   <artifactId>kotlin-client</artifactId>
   <version>0.2.0</version>
</dependency>
    `
  )
  const importSnippet = new Snippet(
    'Imports',
    'kotlin',
    `import com.orbital.client.*`
  )
  const taxiPrimitivesToKotlinTypes = Object.fromEntries([
    [PrimitiveTypeNames.STRING, 'String'],
    [PrimitiveTypeNames.ANY, 'Any'],
    [PrimitiveTypeNames.BOOLEAN, 'Boolean'],
    [PrimitiveTypeNames.DECIMAL, 'BigDecimal'],
    [PrimitiveTypeNames.INSTANT, 'Instant'],
    [PrimitiveTypeNames.TIME, 'LocalTime'],
    [PrimitiveTypeNames.LOCAL_DATE, 'LocalDate'],
    [PrimitiveTypeNames.DOUBLE, 'Double'],
    [PrimitiveTypeNames.INTEGER, 'Int']
  ]);

  function taxiTypeToObjectDefinition(type: Type, schema: Schema, anonymousTypes: Type[]): string[] {
    const types: string[] = [];
    const fields: string[] = [];
    Object.keys(type.attributes).forEach((fieldName: string) => {
      const field = type.attributes[fieldName];
      const fieldType = findType(schema, field.type.parameterizedName, anonymousTypes);

      if (fieldType.isCollection) {
        // todo : Array types
        fieldType.collectionType; // something something
      } else if (fieldType.isScalar) {
        const kotlinType = taxiPrimitivesToKotlinTypes[fieldType.basePrimitiveTypeName.fullyQualifiedName];
        if (isNullOrUndefined(kotlinType)) {
          console.warn(`No kotlin mapping for base primitive type ${fieldType.basePrimitiveTypeName.fullyQualifiedName}`);
        }
        fields.push(`${fieldName} : ${kotlinType}`);
      } else {
        // Object types
        types.push(...taxiTypeToObjectDefinition(fieldType, schema, anonymousTypes));
        fields.push(`${fieldName} : {fieldType.name.shortDisplayName}`);
      }
    });
    types.push(`data class ${type.name.shortDisplayName} (\n   ${fields.join(',\n   ')}\n)`)
    return types;

  }

  const returnTypeCode = taxiTypeToObjectDefinition(request.returnType, request.schema, request.anonymousTypes)
    .join('\n\n');

  const transportSetupCode = `val orbitalClient = http("http://${window.location.host}")`
  // HACK: If this query returns a collection, return a collection
  const returnTypeName: string = (request.query.trim().endsWith('[]') && request.verb === "find") ? `List<${request.returnType.name.shortDisplayName}>` : request.returnType.name.shortDisplayName;


  const reactiveAdaptor = request.verb === "find" ? `.toMono()` : `.toFlux()`

  const queryCode = `query("""${request.query}""")\n.asA<${returnTypeName}>\n.run(orbitalClient)\n${reactiveAdaptor}`
  const indentedCode = queryCode.split('\n').join('\n   ');

  const reactiveType = request.verb === "find" ? `Mono<${returnTypeName}>` : `Flux<${returnTypeName}>`

  const queryFunction = `fun executeQuery():${reactiveType} = ${indentedCode}`
  const code = [returnTypeCode, transportSetupCode, queryFunction].join('\n\n')
  const querySnippet = new Snippet(
    'Code',
    'kotlin',
    code,
    true
  )

  return [
    dependencySnippet,
    importSnippet,
    querySnippet
  ]
}
