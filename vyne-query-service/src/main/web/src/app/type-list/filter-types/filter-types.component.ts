import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {FormBuilder, FormControl, FormGroup} from '@angular/forms';
import {Schema, SchemaMember, SchemaMemberType} from '../../services/schema';
import {TypesService} from '../../services/types.service';
import {ActivatedRoute, Router} from '@angular/router';
import {TypeListComponent} from '../type-list.component';

export interface TypeFilterParams {
  name: string | null;
  namespace: string | null;
  memberType: SchemaMemberType[];
}

export class TypeFilter {
  constructor(private params: TypeFilterParams) {
  }

  filter(members: SchemaMember[]): SchemaMember[] {
    return members
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
  expanded = false;
  filterTypesFormGroup: FormGroup;
  name = new FormControl();
  namespace = new FormControl();
  filterType = new FormControl();
  schema: Schema;
  isFiltered = false;

  filter: TypeFilterParams = {
    name: null,
    memberType: [],
    namespace: null
  };

  @Output()
  filterChanged = new EventEmitter<TypeFilterParams>();


  constructor(fb: FormBuilder, private typeService: TypesService, private activatedRoute: ActivatedRoute, private router: Router) {
    this.activatedRoute.queryParamMap.subscribe(queryParams => {
      const filterFromRoute: TypeFilterParams = {
        name: queryParams.get('name'),
        namespace: queryParams.get('namespace'),
        memberType: queryParams.getAll('memberType') as SchemaMemberType[]
      };
      this.applyFilter(filterFromRoute, false);
    });
    this.filterTypesFormGroup = fb.group({
      name: this.name,
      namespace: this.namespace,
      filterType: this.filterType
    });
  }

  updateFilter() {
    this.applyFilter(this.filter, true);
  }


  private applyFilter(filter: TypeFilterParams, updateRoute: boolean) {
    this.filter = filter;
    this.isFiltered = this.filter.name !== null || this.filter.namespace !== null || this.filter.memberType.length > 0;
    this.filterChanged.emit(filter);
    if (updateRoute) {
      this.setRouteFromFilter(filter);
    }
  }

  clearFilter() {
    this.applyFilter({
      name: null,
      memberType: [],
      namespace: null
    }, true);
    this.filterTypesFormGroup.reset();
  }

  toggleVisibility() {
    this.expanded = !this.expanded;
  }

  get expandWrapperClass(): string {
    return (this.expanded) ? 'panel-visible' : 'panel-hidden';
  }


  private setRouteFromFilter(filter: TypeFilterParams) {
    this.router.navigate(
      [],
      {
        relativeTo: this.activatedRoute,
        queryParams: filter,
      });
  }
}
