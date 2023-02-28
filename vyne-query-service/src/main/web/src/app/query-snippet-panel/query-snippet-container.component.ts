import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, Input, OnInit } from '@angular/core';
import { findType, Schema, Type } from 'src/app/services/schema';
import { Snippet } from 'src/app/query-snippet-panel/query-snippet-panel.component';
import { POLYMORPHEUS_CONTEXT } from '@tinkoff/ng-polymorpheus';
import { TuiDialogContext } from '@taiga-ui/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { PrimitiveTypeNames } from 'src/app/services/taxi';
import { isNullOrUndefined } from 'util';

@Component({
  selector: 'app-query-snippet-container',
  template: `
    <h2>Copy code snippet</h2>
    <div>
      <tui-select
        [stringify]='stringifyGenerator'
        [(ngModel)]='snippetGenerator'
        (ngModelChange)='selectedGeneratorChanged($event)'>
        Select a framework
        <tui-data-list *tuiDataList>
          <button *ngFor='let generator of generators' tuiOption [value]='generator'>{{generator.label}}</button>
        </tui-data-list>
      </tui-select>
    </div>

    <div *ngIf='generatedSnippets' class='generator'>
      <h3>{{snippetGenerator.label}}</h3>
      <app-query-snippet-panel [snippets]='generatedSnippets'></app-query-snippet-panel>
    </div>


  `,
  styleUrls: ['./query-snippet-container.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuerySnippetContainerComponent {

  constructor(private readonly changeDetector: ChangeDetectorRef,
              @Inject(POLYMORPHEUS_CONTEXT) private readonly context: TuiDialogContext<any, CodeGenRequest>
  ) {
    this.queryToGenerate = context.data;
  }

  @Input()
  queryToGenerate: CodeGenRequest;

  generators: SnippetGenerator[] = [
    { label: 'Typescript', generate: typescriptGenerator }
  ];
  snippetGenerator: SnippetGenerator;

  generatedSnippets: Snippet[];

  readonly stringifyGenerator = (item: SnippetGenerator) => item.label;

  selectedGeneratorChanged(generator: SnippetGenerator) {

    this.generatedSnippets = generator.generate(this.queryToGenerate);
    this.changeDetector.markForCheck();
  }
}

export interface CodeGenRequest {
  query: string;
  returnType: Type;
  schema: Schema,

  anonymousTypes: Type[],
  verb: 'find' | 'stream';
}

interface SnippetGenerator {
  label: string;
  generate: (CodeGenRequest) => Snippet[];
}

export function typescriptGenerator(request: CodeGenRequest): Snippet[] {
  const dependency = new Snippet(
    'Dependencies',
    'bash',
    `npm i @orbitalhq/oribtal-client`
  );
  const imports = new Snippet(
    'Boilerplate',
    'typescript',
    `import { HttpQueryClient } from '@orbitalhq/orbital-client';
import { Observable } from 'rxjs';`
  );


  const taxiPrimitivesToTypescriptTypes = Object.fromEntries([
    [PrimitiveTypeNames.STRING, 'string'],
    [PrimitiveTypeNames.ANY, 'any'],
    [PrimitiveTypeNames.BOOLEAN, 'boolean'],
    [PrimitiveTypeNames.DECIMAL, 'number'],
    [PrimitiveTypeNames.INSTANT, 'Date'],
    [PrimitiveTypeNames.TIME, 'Date'],
    [PrimitiveTypeNames.LOCAL_DATE, 'Date'],
    [PrimitiveTypeNames.DOUBLE, 'Date'],
    [PrimitiveTypeNames.INTEGER, 'number']
  ]);

  function taxiTypeToObjectDefinition(type: Type, schema: Schema, anonymousTypes: Type[]): string[] {
    const entries = Object.keys(type.attributes).map((fieldName: string) => {
      const field = type.attributes[fieldName];
      const fieldType = findType(schema, field.type.parameterizedName, anonymousTypes);

      if (fieldType.isCollection) {
        // todo : Array types
        fieldType.collectionType; // something something
      } else if (fieldType.isScalar) {
        const typescriptType = taxiPrimitivesToTypescriptTypes[fieldType.basePrimitiveTypeName.fullyQualifiedName];
        if (isNullOrUndefined(typescriptType)) {
          console.warn(`No typescript mapping for base primitive type ${fieldType.basePrimitiveTypeName.fullyQualifiedName}`);
        }
        return `'${fieldName}' : ${typescriptType}`;
      } else {
        // Object types
        const objectDefinition = taxiTypeToObjectDefinition(fieldType, schema, anonymousTypes);
        const objectDeclaration = objectDefinition.join(',\n');
        return `'${fieldName}' : {\n${objectDeclaration}\n}`;
      }
    });
    return entries;

  }

  const returnTypeMap = taxiTypeToObjectDefinition(request.returnType, request.schema, request.anonymousTypes);
  const returnTypeFields = returnTypeMap.join(',\n');
  const returnTypeDeclaration = `{\n${returnTypeFields}\n}`;

  const functionDefinition = `function loadData(): Observable<${returnTypeDeclaration}> {
   return new HttpQueryClient('${window.origin}').query(\`${request.query}\`);
}
`;
  const functionSnippet = new Snippet('Code', 'typescript', functionDefinition, true);
  return [
    dependency,
    imports,
    functionSnippet
  ] as Snippet[];
}
