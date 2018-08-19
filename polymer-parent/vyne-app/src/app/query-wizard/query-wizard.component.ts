import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {Modifier, Schema, Type, TypeReference, TypesService} from "../services/types.service";
import {map, startWith} from "rxjs/operators";
import {ITdDynamicElementConfig, TdDynamicElement, TdDynamicType} from '@covalent/dynamic-forms';
import {AbstractControl, FormControl} from "@angular/forms";
import {Observable} from "rxjs/internal/Observable";
import {Query, QueryService} from "../services/query.service";

@Component({
  selector: 'app-query-wizard',
  templateUrl: './query-wizard.component.html',
  styleUrls: ['./query-wizard.component.scss'],
})
export class QueryWizardComponent implements OnInit {
  schema: Schema;
  targetTypeInput = new FormControl();
  filteredTypes: Observable<Type[]>;

  private facts: any = {}

  constructor(private route: ActivatedRoute,
              private typesService: TypesService,
              private queryService: QueryService) {
  }

  forms: FactForm[] = [];

  ngOnInit() {
    this.typesService.getTypes()
      .subscribe(next => this.schema = next);
    this.route.queryParamMap
      .subscribe(params => {
          console.log("params: " + params.keys);
          params.getAll("types")
            .forEach(type => this.appendEmptyType(type))
        }
      );

    this.filteredTypes = this.targetTypeInput.valueChanges.pipe(
      startWith(''),
      map(value => this._filter(value))
    );
  }

  private _filter(value: string): Type[] {
    if (!this.schema) return [];
    const filterValue = value.toLowerCase();
    return this.schema.types.filter(option => option.name.fullyQualifiedName.toLowerCase().indexOf(filterValue) !== -1);
  }

  updateFact(formSpec: FactForm, formInstance: AbstractControl) {
    let nestedValue = this.nest(formInstance.value)

    this.facts[formSpec.type.name.fullyQualifiedName] = nestedValue;
  }

  submitQuery() {
    let query = new Query(
      this.targetTypeInput.value,
      this.facts
    );
    this.queryService.submitQuery(query)
      .subscribe(result => console.log(result))
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
      debugger;
    }

    let control: any;
    switch (targetType) {
      case "lang.taxi.String" :
        control = {type: TdDynamicElement.Input};
        break;
      case "lang.taxi.Decimal" :
        control = {type: TdDynamicType.Number};
        break;
      case "lang.taxi.Integer" :
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


export class FactForm {
  constructor(
    readonly elements: ITdDynamicElementConfig[],
    readonly  type: Type
  ) {
  }
}
