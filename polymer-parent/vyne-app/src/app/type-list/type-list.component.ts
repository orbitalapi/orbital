import {Component, OnInit} from '@angular/core';
import {Schema, SchemaMember, SchemaMemberType, Service, Type, TypesService} from "../services/types.service";
import * as _ from "lodash";
import {Router} from "@angular/router";

@Component({
  selector: 'app-type-list',
  templateUrl: './type-list.component.html',
  styleUrls: ['./type-list.component.scss']
})
export class TypeListComponent implements OnInit {

  constructor(private typeService: TypesService, private router: Router) {
  }

  schema: Schema;
  members: SchemaMember[] = [];

  ngOnInit() {
    this.loadTypes()
  }

  private loadTypes() {
    this.typeService.getTypes().subscribe(
      schema => {
        let typeMembers: SchemaMember[] = schema.types.map((t) => SchemaMember.fromType(t as Type));
        let operationMembers: SchemaMember[] = [];
        schema.services.forEach((service) => operationMembers = operationMembers.concat(SchemaMember.fromService(service as Service)));
        let members: SchemaMember[] = typeMembers.concat(operationMembers);
        members = _.sortBy(members, [(m: SchemaMember) => {
          return m.name.fullyQualifiedName
        }]);
        this.schema = schema;
        this.members = members
      },
      error => console.log("error : " + error)
    );
  }

  memberType(member: SchemaMember): string {
    if (member.kind == SchemaMemberType.OPERATION) return "Operation";
    if (member.kind == SchemaMemberType.TYPE) return "Type";
    if (member.kind == SchemaMemberType.SERVICE) return "Service";
    return "?"
  }

  startNewQuery(member: SchemaMember) {
    this.router.navigate(["/query-wizard"], {
      queryParams: {"types": [member.name.fullyQualifiedName]}
    })
  }
}
