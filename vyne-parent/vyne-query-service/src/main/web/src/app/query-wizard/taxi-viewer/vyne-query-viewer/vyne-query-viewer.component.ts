import {Component, Input, OnInit} from '@angular/core';
import * as _ from "lodash";
import {QualifiedName, Schema} from "../types.service";
import {Fact, Query, QueryMode} from "../query.service";

@Component({
  selector: 'app-vyne-query-viewer',
  templateUrl: './vyne-query-viewer.component.html',
  styleUrls: ['./vyne-query-viewer.component.scss']
})
export class VyneQueryViewerComponent implements OnInit {

  private generators: Generator[] = [
    new KotlinGenerator(),
    new TypescriptGenerator(),
    new JsonGenerator()
  ];

  private _schema: any;
  private _facts: Fact[];
  private _targetType: string;
  private _queryMode: QueryMode;

  @Input()
  expanded: boolean = true;

  get facts(): Fact[] {
    return this._facts;
  }

  @Input()
  set facts(value: Fact[]) {
    this._facts = value;
    this.generateCode();
  }

  @Input()
  get schema(): any {
    return this._schema;
  }

  set schema(value: any) {
    this._schema = value;
    this.generateCode();
  }


  get targetType(): string {
    return this._targetType;
  }

  @Input()
  set targetType(value: string) {
    this._targetType = value;
    this.generateCode();
  }

  @Input()
  get queryMode(): QueryMode {
    return this._queryMode;
  }

  set queryMode(value: QueryMode) {
    this._queryMode = value;
    this.generateCode();
  }

  snippets: Snippet[] = [];
  activeSnippet: Snippet;

  private generateCode() {
    if (!this._schema || !this.facts || !this._targetType || !this._queryMode) {
      return
    }

    this.snippets = this.generators.map(generator => generator.generate(this._schema, this.facts, this._targetType, this._queryMode));
    this.activeSnippet = this.snippets[0];
  }


  ngOnInit(): void {
    this.generateCode();
  }


  selectSnippet(snippet: Snippet, $event) {
    this.activeSnippet = snippet;
    $event.stopPropagation();
    $event.stopImmediatePropagation();
  }

  classForSnippet(snippet: Snippet): string {
    return (this.activeSnippet === snippet) ? "active" : "";
  }
}

class Snippet {
  constructor(readonly displayLang: string, readonly formatterLang: string, readonly  content: string) {

  }
}

interface Generator {
  generate(schema: Schema, facts: Fact[], targetType: string, queryMode: QueryMode): Snippet
}


class JsonGenerator implements Generator {
  generate(schema: Schema, facts: Fact[], targetType: string, queryMode: QueryMode): Snippet {
    const query = new Query(targetType, facts, queryMode);
    return new Snippet("json", "json", JSON.stringify(query, null, 3));
  }

}


abstract class ObjectScalarGenerator {
  getFacts(schema: Schema, facts: Fact[]): string {
    let factsCode = facts.map(fact => {
      const factType = schema.types.find(t => t.name.fullyQualifiedName === fact.typeName);
      if (!factType) {
        console.error(`Fact ${fact.typeName} was not found!`)
        return "error";
      }
      if (factType.scalar) {
        return this.generateScalarFact(fact);
      } else {
        return this.generateObjectType(fact);
      }
    }).join("\n");
    return factsCode
  }

  protected abstract generateScalarFact(fact: Fact): string

  protected abstract generateObjectType(fact: Fact): string
}

class TypescriptGenerator extends ObjectScalarGenerator implements Generator {
  generate(schema: Schema, facts: Fact[], targetType: string, queryMode: QueryMode): Snippet {
    let factsCode = this.getFacts(schema, facts);
    const code = `
${factsCode}
const query = new Query("${targetType}", [fact], QueryMode.${queryMode})
queryService.submit(query).subscribe(result => console.log(result))
    `.trim();
    return new Snippet("Typescript", "typescript", code);

  }

  protected generateObjectType(fact: Fact): string {
    return `const fact:Fact = new Fact("${fact.typeName}","${fact.value}")`
  }

  protected generateScalarFact(fact: Fact): string {
    return `const fact:Fact = new Fact("${fact.typeName}","${fact.value}")`
  }
}


class KotlinGenerator extends ObjectScalarGenerator implements Generator {
  generate(schema: Schema, facts: Fact[], targetType: string, queryMode: QueryMode): Snippet {
    let factsCode = this.getFacts(schema, facts);

    let targetTypeClassName = QualifiedName.nameOnly(targetType);
    let method = (queryMode == QueryMode.DISCOVER) ? "discover" : "gather"
    const code = `
${factsCode}
vyne.given(fact).${method}<${targetTypeClassName}>()
    `.trim();
    return new Snippet("Kotlin", "kotlin", code);
  }

  protected generateScalarFact(fact: Fact): string {
    const className = _.upperFirst(QualifiedName.nameOnly(fact.typeName));
    return `val fact = "${fact.value}".as<${className}>()`
  }

  protected generateObjectType(fact: Fact): string {
    const className = _.capitalize(QualifiedName.nameOnly(fact.typeName));
    const props = Object.keys(fact.value).map(key => `${key} = ${fact[key]}`).join(", ")
    return `val fact = ${className}(${props})`
  }
}

