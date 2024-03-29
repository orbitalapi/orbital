import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {EnumValues, findType, Modifier, Schema, Type} from '../../services/schema';
import {ParsedTypeInstance, TypesService} from '../../services/types.service';
import {map} from 'rxjs/operators';
import {
  ITdDynamicElementConfig,
  TdDynamicElement,
  TdDynamicFormsComponent,
  TdDynamicType
} from '@covalent/dynamic-forms';
import {FormControl} from '@angular/forms';
import {
  Fact,
  Query,
  QueryMode,
  QueryProfileData,
  RemoteCall,
  ResponseStatus,
  ResultMode
} from '../../services/query.service';
import {FailedSearchResponse} from '../../services/models';


@Component({
  selector: 'query-wizard',
  templateUrl: './query-wizard.component.html',
  styleUrls: ['./query-wizard.component.scss'],
})
export class QueryWizardComponent implements OnInit {
  schema: Schema;
  queryMode = new FormControl();

  targetTypes: Type[];
  findAsArray = false;

  constructor(private route: ActivatedRoute,
              private typesService: TypesService) {
  }

  forms: FactForm[] = [];
  facts: Fact[] = [];

  // fakeFacts:Fact[] = [];
  private subscribedDynamicForms: TdDynamicFormsComponent[] = [];

  @Output()
  executeQuery = new EventEmitter<Query>();

  addingNewFact = false;

  get targetTypeNames(): string[] {
    if (!this.targetTypes) {
      return [];
    }
    return this.targetTypes.map(t => t.name.fullyQualifiedName);
  }


  ngOnInit() {
    this.typesService.getTypes()
      .subscribe(next => this.schema = next);
    this.route.queryParamMap
      .subscribe(params => {
          params.getAll('types')
            .forEach(type => this.appendEmptyType(type));
        }
      );

    this.queryMode.setValue(QueryMode.DISCOVER);
  }

  // Dirty hack to capture the forms generated dynamically, so we can listen for
  // form events
  getAndRegisterElements(factForm: FactForm, component: TdDynamicFormsComponent) {
    if (this.subscribedDynamicForms.indexOf(component) === -1) {
      component.form.valueChanges.subscribe(valueChangedEvent => {
        this.updateFact(factForm, component.value);
      });
      this.subscribedDynamicForms.push(component);
    }

    return factForm.elements;
  }

  removeFact(factForm: FactForm) {
    const index = this.forms.indexOf(factForm);
    this.forms.splice(index, 1);
    this.facts = this.buildFacts();
  }

  updateFact(formSpec: FactForm, value) {
    formSpec.value = value;

    // let fullyQualifiedName = formSpec.type.name.fullyQualifiedName;
    // TODO: Add a test for this.
    // The issue I found was that when the type passed in to the
    // query builder is a simple type (ie., CustomerNumber),
    // then the request was being built incorrectly, as
    // CustomerNumber : { CustomerNumber : 1 }
    // instead of :
    // CustomerNumber : 1
    // This whole thing is a smell, and need to get some decent
    // tests around this.
    // if (formSpec.type.scalar) {
    //   let unwrappedValue = Object.values(value)[0];
    //   this.facts[fullyQualifiedName] = unwrappedValue;
    // } else {
    //   let nestedValue = this.nest(value);
    //   this.facts[fullyQualifiedName] = nestedValue;
    // }

    this.facts = this.buildFacts();
  }

  private buildFacts(): Fact[] {
    // const facts = {};
    const formFacts: Fact[] = this.forms
      .filter(formSpec => formSpec.value)
      .map(formSpec => {
        const fullyQualifiedName = formSpec.type.name.fullyQualifiedName;
        if (formSpec.type.isScalar) {
          const unwrappedValue = Object.values(formSpec.value)[0];
          return new Fact(fullyQualifiedName, unwrappedValue);
          // facts[fullyQualifiedName] = unwrappedValue;

        } else {
          const nestedValue = this.nest(formSpec.value);
          return new Fact(fullyQualifiedName, nestedValue);
          // facts[fullyQualifiedName] = nestedValue;
        }
      });


    return formFacts;
  }

  submitQuery() {
    const factList = this.buildFacts();
    const query = new Query(
      {
        typeNames: this.targetTypes.map(t => this.findAsArray ? `${t.name.fullyQualifiedName}[]` : t.name.fullyQualifiedName),
      },
      // this.targetTypeInput.value,
      factList,
      this.queryMode.value,
      ResultMode.SIMPLE
    );

    this.executeQuery.emit(query);
  }

  // Convert a property of "foo.bar = 123" to an object with nested properties
  private nest(value: any): any {
    const result = {};
    Object.keys(value).forEach(attriubteName => {
      this.setValue(result, attriubteName, value[attriubteName]);
    });
    return result;
  }

  private setValue(target: any, path: string, value: any) {
    if (path.indexOf('.') === -1) {
      target[path] = value;
    } else {
      const pathParts = path.split('.');
      const thisPart = pathParts.splice(0, 1)[0];
      if (!target.hasOwnProperty(thisPart)) {
        target[thisPart] = {};
      }
      this.setValue(target[thisPart], pathParts.join('.'), value);
    }
  }

  private appendEmptyType(typeName: string) {
    this.typesService.getTypes()
      .pipe(
        map(schema => {
          const type = schema.types.find(schemaType => schemaType.name.fullyQualifiedName === typeName);
          return this.buildTypeForm(type, schema);
        }),
      ).subscribe(form => this.forms.push(form));
  }

  private buildTypeForm(type: Type, schema: Schema): FactForm {
    const elements = this.getElementsForType(type, schema);
    return new FactForm(
      elements,
      type
    );
  }

  private getElementsForType(type: Type, schema: Schema, prefix: string[] = [], fieldName: string = null): ITdDynamicElementConfig[] {
    if (type.isScalar) {
      // suspect this is a smell I'm doing something wrong.
      // If the original root type was scalar, when we won't have a prefix, so
      // just use the name directly.
      // Otherwise, if we've navigated into this attribute, use the prefix, which is actually
      // the full path.
      const name = (prefix.length === 0) ? type.name.name : prefix.join('.');
      const label = (fieldName) ? `${fieldName} (${type.name.name})` : type.name.name;
      return [{
        name: name,
        label: label,
        ...this.getInputControlForType(type)
      }];
    } else {
      let elements: ITdDynamicElementConfig[] = [];
      Object.keys(type.attributes).forEach(attributeName => {
        const attributeTypeRef = type.attributes[attributeName];
        const attributeType = findType(schema, attributeTypeRef.type.fullyQualifiedName);
        const newPrefix = prefix.concat([attributeName]);
        elements = elements.concat(this.getElementsForType(attributeType, schema, newPrefix, attributeName));
      });
      return elements;
    }
  }

  private findRootTypeName(type: Type): string {
    const targetType = (type.aliasForType) ? type.aliasForType.fullyQualifiedName : type.name.fullyQualifiedName;
    if (type.basePrimitiveTypeName) {
      return type.basePrimitiveTypeName.fullyQualifiedName;
    }
    if (type.inheritsFrom && type.inheritsFrom.length > 0) {
      // Example:
      // type OrderId inherits String
      const parentTypeFullyQualifiedName = type.inheritsFrom[0].fullyQualifiedName;
      const parentType = this.schema.types.find(schemaType => schemaType.name.fullyQualifiedName === parentTypeFullyQualifiedName);
      if (parentType) {
        return this.findRootTypeName(parentType);
      }
    }
    return targetType;
  }

  private getInputControlForType(type: Type): any {
    if (!type.isScalar) {
      throw new Error('Can only get inputs for scalar types');
    }
    // TODO : Aliases could be nested ... follow the chain
    let targetType = (type.aliasForType) ? type.aliasForType.fullyQualifiedName : type.name.fullyQualifiedName;
    if (type.modifiers.indexOf('ENUM') !== -1) {

      return {
        type: TdDynamicElement.Select,
        selections: type.enumValues.map((enumValue: EnumValues) => {
          return {
            label: `${enumValue.name} (${enumValue.value})`,
            value: enumValue.value ? enumValue.value : enumValue.name
          };
        })
      };
    }

    targetType = this.findRootTypeName(type);
    let control: any;
    switch (targetType) {
      case 'lang.taxi.String' :
        control = {type: TdDynamicElement.Input};
        break;
      case 'lang.taxi.Decimal' :
        control = {type: TdDynamicType.Number};
        break;
      case 'lang.taxi.Int' :
        control = {type: TdDynamicType.Number};
        break;
      case 'lang.taxi.Boolean' :
        control = {type: TdDynamicElement.Checkbox};
        break;
      default:
        console.error('Unhandled type in getInputControlForType()');
        break;
    }
    return control;
  }


  addNewFact() {
    this.addingNewFact = true;
  }

  onNewFactTypeSelected(type: Type) {
    this.appendEmptyType(type.name.fullyQualifiedName);
    this.addingNewFact = false;
  }

  handleFileFactTypeSelected(fileFact: FileFactForm, type: Type) {
    fileFact.type = type;
    this.facts = this.buildFacts();
  }

  handleFileFactChanged(fileFact: FileFactForm, parsedTypeInstance: ParsedTypeInstance) {
    fileFact.instance = parsedTypeInstance;
    this.facts = this.buildFacts();
  }

}

/**
 * @deprecated use FailedSearchResponse instead
 */
export class QueryFailure implements FailedSearchResponse {
  responseStatus: ResponseStatus = ResponseStatus.ERROR;

  constructor(readonly message: string,
              readonly profilerOperation: QueryProfileData | null = null,
              readonly remoteCalls: RemoteCall[] = [],
              readonly queryResponseId: string | null = null,
              readonly  clientQueryId: string | null = null) {
  }

}


export class FactForm {
  constructor(
    readonly elements: ITdDynamicElementConfig[],
    readonly  type: Type
  ) {

  }

  value: any;

}


export class FileFactForm {
  instance: ParsedTypeInstance = null;
  type: Type = null;

  asFact(): Fact {
    return new Fact(this.type.name.fullyQualifiedName, this.instance.raw);
  }

  hasFact(): boolean {
    return this.instance !== null && this.type !== null;
  }

  hasValue(): boolean {
    return this.instance !== null;
  }
}
