import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {Modifier, Schema, Type, TypeReference, TypesService} from "../services/types.service";
import {map, startWith} from "rxjs/operators";
import {
  ITdDynamicElementConfig,
  TdDynamicElement,
  TdDynamicFormsComponent,
  TdDynamicType
} from '@covalent/dynamic-forms';
import {FormControl} from "@angular/forms";
import {Observable} from "rxjs/internal/Observable";
import {
  Fact,
  ProfilerOperation,
  Query,
  QueryMode,
  QueryResult,
  QueryService,
  RemoteCall
} from "../services/query.service";
import {HttpErrorResponse} from "@angular/common/http";

@Component({
  selector: 'query-wizard',
  templateUrl: './query-wizard.component.html',
  styleUrls: ['./query-wizard.component.scss'],
})
export class QueryWizardComponent implements OnInit {
  schema: Schema;
  targetTypeInput = new FormControl();
  queryMode = new FormControl();
  filteredTypes: Observable<Type[]>;

  private facts: any = {};

  constructor(private route: ActivatedRoute,
              private typesService: TypesService,
              private queryService: QueryService) {
  }

  forms: FactForm[] = [];

  private subscribedDynamicForms: TdDynamicFormsComponent[] = [];

  lastQueryResult: QueryResult | QueryFailure;

  ngOnInit() {
    this.typesService.getTypes()
      .subscribe(next => this.schema = next);
    this.route.queryParamMap
      .subscribe(params => {
          params.getAll("types")
            .forEach(type => this.appendEmptyType(type))
        }
      );

    this.filteredTypes = this.targetTypeInput.valueChanges.pipe(
      startWith(''),
      map(value => this._filter(value))
    );

    this.queryMode.setValue(QueryMode.DISCOVER)
  }

  // Dirty hack to capture the forms generated dynamically, so we can listen for
  // form events
  getAndRegisterElements(factForm: FactForm, component: TdDynamicFormsComponent) {
    if (this.subscribedDynamicForms.indexOf(component) == -1) {
      component.form.valueChanges.subscribe(valueChangedEvent => {
        this.updateFact(factForm, component.value);
      });
      this.subscribedDynamicForms.push(component)
    }

    return factForm.elements;
  }

  removeFact(factForm: FactForm) {
    console.error("TODO!")
  }

  private _filter(value: string): Type[] {
    if (!this.schema) return [];
    const filterValue = value.toLowerCase();
    return this.schema.types.filter(option => option.name.fullyQualifiedName.toLowerCase().indexOf(filterValue) !== -1);
  }

  updateFact(formSpec: FactForm, value) {
    let fullyQualifiedName = formSpec.type.name.fullyQualifiedName;
    // TODO: Add a test for this.
    // The issue I found was that when the type passed in to the
    // query builder is a simple type (ie., CustomerNumber),
    // then the request was being built incorrectly, as
    // CustomerNumber : { CustomerNumber : 1 }
    // instead of :
    // CustomerNumber : 1
    // This whole thing is a smell, and need to get some decent
    // tests around this.
    if (formSpec.type.scalar) {
      let unwrappedValue = Object.values(value)[0];
      this.facts[fullyQualifiedName] = unwrappedValue;
    } else {
      let nestedValue = this.nest(value);
      this.facts[fullyQualifiedName] = nestedValue;
    }
  }

  submitQuery() {
    this.lastQueryResult = null;
    let factList: Fact[] = Object.keys(this.facts).map(key => new Fact(key, this.facts[key]));
    let query = new Query(
      this.targetTypeInput.value,
      factList,
      this.queryMode.value
    );
    this.queryService.submitQuery(query)
      .subscribe(result => {
        this.lastQueryResult = result
      }, error => {
        let errorResponse = error as HttpErrorResponse;
        if (errorResponse.error && (errorResponse.error as any).hasOwnProperty('profilerOperation')) {
          this.lastQueryResult = new QueryFailure(errorResponse.error.message, errorResponse.error.profilerOperation, errorResponse.error.remoteCalls)
        } else {
          // There was an unhandled error...
          console.error("An unhandled error occurred:");
          console.error(JSON.stringify(error));
        }
      })
  }

  // Convert a property of "foo.bar = 123" to an object with nested properties
  private nest(value: any): any {
    let result = {};
    Object.keys(value).forEach(attriubteName => {
      this.setValue(result, attriubteName, value[attriubteName]);
    });
    return result;
  }

  private setValue(target: any, path: string, value: any) {
    if (path.indexOf('.') === -1) {
      target[path] = value
    } else {
      let pathParts = path.split('.');
      let thisPart = pathParts.splice(0, 1)[0];
      if (!target.hasOwnProperty(thisPart)) {
        target[thisPart] = {};
      }
      this.setValue(target[thisPart], pathParts.join("."), value);
    }
  }

  private appendEmptyType(typeName: string) {
    this.typesService.getTypes()
      .pipe(
        map(schema => {
          let type = schema.types.find(type => type.name.fullyQualifiedName == typeName);
          return this.buildTypeForm(type, schema)
        }),
      ).subscribe(form => this.forms.push(form))
  }

  private buildTypeForm(type: Type, schema: Schema): FactForm {
    let elements = this.getElementsForType(type, schema);
    return new FactForm(
      elements,
      type
    )
  }

  private getElementsForType(type: Type, schema: Schema, prefix: string[] = []): ITdDynamicElementConfig[] {
    if (type.scalar) {
      // suspect this is a smell I'm doing something wrong.
      // If the original root type was scalar, when we won't have a prefix, so
      // just use the name directly.
      // Otherwise, if we've navigated into this attribute, use the prefix, which is actually
      // the full path.
      let name = (prefix.length == 0) ? type.name.name : prefix.join(".");
      return [{
        name: name,
        label: type.name.name,
        ...this.getInputControlForType(type)
      }];
    } else {
      let elements: ITdDynamicElementConfig[] = [];
      Object.keys(type.attributes).forEach(attributeName => {
        let attributeTypeRef = type.attributes[attributeName] as TypeReference;
        let attributeType = schema.types.find(type => type.name.fullyQualifiedName == attributeTypeRef.fullyQualifiedName);
        let newPrefix = prefix.concat([attributeName]);
        elements = elements.concat(this.getElementsForType(attributeType, schema, newPrefix));
      });
      return elements;
    }
  }

  private getInputControlForType(type: Type): any {
    if (!type.scalar) {
      throw new Error("Can only get inputs for scalar types");
    }
    // TODO : Aliases could be nested ... follow the chain
    let targetType = (type.aliasForType) ? type.aliasForType.fullyQualifiedName : type.name.fullyQualifiedName
    if (type.modifiers.indexOf(Modifier.ENUM) != -1) {
      return {
        type: TdDynamicElement.Select,
        selections: type.enumValues
      }
    }

    let control: any;
    switch (targetType) {
      case "lang.taxi.String" :
        control = {type: TdDynamicElement.Input};
        break;
      case "lang.taxi.Decimal" :
        control = {type: TdDynamicType.Number};
        break;
      case "lang.taxi.Int" :
        control = {type: TdDynamicType.Number};
        break;
      case "lang.taxi.Boolean" :
        control = {type: TdDynamicElement.Checkbox};
        break;
      default:
        debugger;
    }
    return control;
  }


}

export class QueryFailure {
  constructor(readonly message: string, readonly profilerOperation: ProfilerOperation, readonly remoteCalls: RemoteCall[]) {
  }
}


export class FactForm {
  constructor(
    readonly elements: ITdDynamicElementConfig[],
    readonly  type: Type
  ) {
  }
}
