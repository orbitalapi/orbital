import { Component, OnInit } from '@angular/core';
import { TypesService } from '../services/types.service';
import * as _ from 'lodash';
import { Router } from '@angular/router';
import { Schema, SchemaMember, SchemaMemberType, Service, Type } from '../services/schema';
import { TypeFilter, TypeFilterParams } from './filter-types/filter-types.component';
import { SchemaNotificationService } from '../services/schema-notification.service';

@Component({
  selector: 'app-type-list',
  templateUrl: './type-list.component.html',
  styleUrls: ['./type-list.component.scss']
})
export class TypeListComponent implements OnInit {

  constructor(private typeService: TypesService,
              private router: Router) {
  }

  schema: Schema;
  members: SchemaMember[] = [];
  filteredMembers: SchemaMember[] = [];
  filterProps: TypeFilterParams;

  ngOnInit() {
    this.loadTypes();
  }

  type(schemaMember: SchemaMember): Type {
    return this.schema.types.find((t) => t.name.fullyQualifiedName === schemaMember.name.fullyQualifiedName);
  }

  private loadTypes(refresh: boolean = false) {
    this.typeService.getTypes(refresh).subscribe(schema => {
        this.schema = schema;
        this.members = this.buildUnfilteredMembers(schema);
        this.applyFilter();
      }, error => console.log('error : ' + error)
    );
  }

  private buildUnfilteredMembers(schema: Schema): SchemaMember[] {
    const typeMembers: SchemaMember[] = schema.types.map((t) => SchemaMember.fromType(t as Type));
    let operationMembers: SchemaMember[] = [];
    this.schema.services.forEach((service) => operationMembers = operationMembers.concat(SchemaMember.fromService(service as Service)));
    return typeMembers.concat(operationMembers);
  }

  private applyFilter() {
    // Filter
    let members = (this.filterProps) ? new TypeFilter(this.filterProps).filter(this.members) : this.members;
    // Sort
    members = _.sortBy(members, [(m: SchemaMember) => {
      return m.name.fullyQualifiedName;
    }]);
    this.filteredMembers = members;
  }

  memberType(member: SchemaMember): string {
    if (member.kind === 'OPERATION') {
      return 'Operation';
    }
    if (member.kind === 'TYPE') {
      return 'Type';
    }
    if (member.kind === 'SERVICE') {
      return 'Service';
    }
    return '?';
  }

  startNewQuery(member: SchemaMember) {
    this.router.navigate(['/query-wizard'], {
      queryParams: { 'types': [member.name.fullyQualifiedName] }
    });
  }


  navigateToMember(member: SchemaMember) {
    switch (member.kind) {
      case 'SERVICE':
        this.router.navigate(['/services', member.name.fullyQualifiedName]);
        break;
      case 'OPERATION':
        const parts = member.name.fullyQualifiedName
          .replace('@@', '/')
          .split('/');
        const serviceName = parts[0].trim();
        const operationName = parts[1].trim();
        this.router.navigate(['/services', serviceName, operationName]);
        break;
      default:
        this.router.navigate(['/catalog', member.name.fullyQualifiedName]);
    }

  }

  refresh() {
    this.typeService.getTypes(true);
  }

  updateFilter($event: TypeFilterParams) {
    this.filterProps = $event;
    this.applyFilter();
  }


}
