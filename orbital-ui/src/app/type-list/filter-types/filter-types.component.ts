import { Component, EventEmitter, Input, Output } from '@angular/core';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { Schema, SchemaMember, SchemaMemberKind } from '../../services/schema';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';

export const SHOW_EVERYTHING: TypeFilterParams = {
  name: null,
  namespace: null,
  memberType: []
};

export interface TypeFilterParams {
  name: string | null;
  namespace: string | null;
  memberType: SchemaMemberKind[];
}

export class TypeFilter {
  constructor(private params: TypeFilterParams) {
  }

  excludedNamespaces = [
    // TODO : We have a bunch of demos
    // that use the io.vyne namespace,
    // so for now we can't just exclude the entire parent.
    'io.vyne.catalog',
    'io.vyne.jdbc',
    'io.vyne.formats',
    'io.vyne.kafka',
    'io.vyne.aws',
    'io.vyne.azure',
    'lang.taxi',
    'taxi.stdlib',
    'vyne.vyneQl',
    'io.vyne.Username',
    'io.vyne.Error'
  ];

  filter(members: SchemaMember[]): SchemaMember[] {
    return members
      .filter(v => !this.excludedNamespaces.some(namespace => v.name.fullyQualifiedName.startsWith(namespace)))

      .filter(v => this.typeFilter(v))
      .filter(v => this.nameFilter(v))
      .filter(v => this.namespaceFilter(v));
  }

  private nameFilter(value: SchemaMember): boolean {
    if (!this.params.name) {
      return true;
    } else {
      return value.name.name.toLowerCase().includes(this.params.name.toLowerCase());
    }
  }

  private namespaceFilter(value: SchemaMember): boolean {
    if (!this.params.namespace) {
      return true;
    } else {
      return value.name.namespace.toLowerCase().includes(this.params.namespace.toLowerCase());
    }
  }

  private typeFilter(value: SchemaMember): boolean {
    if (!this.params.memberType || this.params.memberType.length === 0) {
      return true;
    } else {
      return this.params.memberType.indexOf(value.kind) !== -1;
    }
  }
}

@Component({
  selector: 'app-filter-types',
  templateUrl: './filter-types.component.html',
  styleUrls: ['./filter-types.component.scss']
})


export class FilterTypesComponent {
  @Input()
  expanded: boolean;
  filterTypesFormGroup: UntypedFormGroup;
  name = new UntypedFormControl();
  namespace = new UntypedFormControl();
  filterType = new UntypedFormControl();
  schema: Schema;
  isFiltered = false;

  filter: TypeFilterParams = {
    name: null,
    memberType: [],
    namespace: null
  };

  @Output()
  filterChanged = new EventEmitter<TypeFilterParams>();

  formGroup: UntypedFormGroup;


  constructor(fb: UntypedFormBuilder, private activatedRoute: ActivatedRoute, private router: Router, private location: Location) {
    this.formGroup = fb.group({
      filter: fb.control(''),
      showTypes: fb.control(true),
      showServices: fb.control(true),
      showOperations: fb.control(true)
    });

    this.activatedRoute.queryParamMap.subscribe(queryParams => {
      const memberTypes = (queryParams.getAll('memberType') || []) as SchemaMemberKind[];
      const memberTypesIsEmpty = memberTypes.length === 0;
      const formValue = {
        filter: queryParams.get('name'),
        showTypes: memberTypesIsEmpty || memberTypes.includes('TYPE'),
        showServices: memberTypesIsEmpty || memberTypes.includes('SERVICE'),
        showOperations: memberTypesIsEmpty || memberTypes.includes('OPERATION')
      };
      this.formGroup.setValue(formValue, { emitEvent: false });
    });

    this.formGroup.valueChanges
      .subscribe(result => {
        const types: SchemaMemberKind[] = [];
        if (result['showTypes']) types.push('TYPE');
        if (result['showServices']) types.push('SERVICE');
        if (result['showOperations']) types.push('OPERATION');

        this.applyFilter({
          name: result['filter'],
          memberType: types,
          namespace: null
        }, true);
      });
  }

  private applyFilter(filter: TypeFilterParams, updateRoute: boolean) {
    this.filterChanged.emit(filter);
    if (updateRoute) {
      this.setRouteFromFilter(filter);
    }
  }

  private setRouteFromFilter(filter: TypeFilterParams) {
    const url = this.router.createUrlTree(
      [],
      {
        relativeTo: this.activatedRoute,
        queryParams: filter
      }).toString();
    this.location.go(url);
  }
}
