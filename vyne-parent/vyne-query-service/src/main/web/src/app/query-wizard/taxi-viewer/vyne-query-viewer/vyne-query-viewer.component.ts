import {Component, Input, NgZone, OnInit} from '@angular/core';
import * as _ from "lodash";
import {Fact, Query, QueryMode} from "../../../services/query.service";
import {QualifiedName, Schema} from "../../../services/schema";

// import {Fact, Query, QueryMode} from "../query.service";

@Component({
  selector: 'app-vyne-query-viewer',
  templateUrl: './vyne-query-viewer.component.html',
  styleUrls: ['./vyne-query-viewer.component.scss']
})
export class VyneQueryViewerComponent implements OnInit {

  constructor(private zone: NgZone) {

  }

  private generators: Generator[] = [
    new KotlinGenerator(),
    new TypescriptGenerator(),
    new JsonGenerator(),
    new GoogleDocsGenerator()
  ];

  private _schema: any;
  private _facts: Fact[];
  private _targetTypes: string[];
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


  get targetTypes(): string[] {
    return this._targetTypes;
  }

  @Input()
  set targetTypes(value: string[]) {
    this._targetTypes = value;
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
  activeSnippet: Snippet = new Snippet("", "typescript", "");

  private generateCode() {
    if (!this._schema || !this.facts || !this._targetTypes || !this._queryMode) {
      return
    }

    this.snippets = this.generators.map(generator => generator.generate(this._schema, this.facts, this._targetTypes, this._queryMode));
  }


  ngOnInit(): void {
    this.generateCode();
    this.activeSnippet = this.snippets[0];
  }


  selectSnippet(snippet: Snippet, $event) {
    // this.zone.runGuarded(() => {
    console.log("Language selected");
    this.activeSnippet = snippet;
    console.log(this.activeSnippet.content);
    $event.stopPropagation();
    $event.stopImmediatePropagation();
    // })
  }

  classForSnippet(snippet: Snippet): string {
    return (this.activeSnippet.displayLang === snippet.displayLang) ? "active" : "";
  }
}

class Snippet {
  constructor(readonly displayLang: string, readonly formatterLang: string, readonly  content: string) {

  }
}

interface Generator {
  generate(schema: Schema, facts: Fact[], targetTypes: string[], queryMode: QueryMode): Snippet
}


class JsonGenerator implements Generator {
  generate(schema: Schema, facts: Fact[], targetType: string[], queryMode: QueryMode): Snippet {
    const query = new Query(targetType, facts, queryMode);
    return new Snippet("json", "json", JSON.stringify(query, null, 3));
  }

}

class GoogleDocsGenerator implements Generator {
  generate(schema: Schema, facts: Fact[], targetTypes: string[], queryMode: QueryMode): Snippet {
    let targetType = targetTypes[0];
    let formula: string;
    if (targetTypes.length > 1) {
      formula = "Multiple query targets not supported in Google Sheets";
    } else if (facts.length == 0) {
      formula = `=discover("${targetType}")`
    } else if (facts.length === 1) {
      const fact = facts[0];
      formula = `=discover("${targetType}","${QualifiedName.nameOnly(fact.typeName)}","${fact.value}")`
    } else {
      formula = "Error - Google docs only supports queries with a single fact";
    }
    return new Snippet("Google Sheets", "typescript", formula);
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
  generate(schema: Schema, facts: Fact[], targetType: string[], queryMode: QueryMode): Snippet {
    let factsCode = this.getFacts(schema, facts);
    const code = `
${factsCode}
const query = new Query("[${targetType.join(",")}]", [fact], QueryMode.${queryMode})
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
  generate(schema: Schema, facts: Fact[], targetType: string[], queryMode: QueryMode): Snippet {
    let factsCode = this.getFacts(schema, facts);

    let method = (queryMode == QueryMode.DISCOVER) ? "discover" : "gather";
    let discoveryCode: string;


    if (targetType.length == 1) {
      discoveryCode = this.getSingleDiscoveryCode(targetType[0], method)
    } else {
      discoveryCode = this.getMultiDiscoveryCode(targetType, method)
    }

    let code = factsCode + '\n' + discoveryCode;

    return new Snippet("Kotlin", "kotlin", code);
  }

  private getSingleDiscoveryCode(targetType: string, method: string) {
    let targetTypeClassName = QualifiedName.nameOnly(targetType);
    return `vyne.given(fact).${method}<${targetTypeClassName}>()`.trim();
  }

  private getMultiDiscoveryCode(targetType: string[], method: string) {
    let typeNames = targetType.map(t => `"${t}"`).join(", ");
    return `vyne.given(fact).${method}(listOf(${typeNames}))`.trim();
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

