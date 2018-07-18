import {Component, ElementRef, HostListener, OnInit} from "@angular/core";
import * as _ from "lodash";
import {Operation, QualifiedName, Schema, Service, SourceCode, Type, TypesService} from "./types.service";
// import {colorSets} from '@swimlane/ngx-graph/src/utils'
import {FormControl} from "@angular/forms";

@Component({
    selector: ".content_inner_wrapper",
    templateUrl: "./type-explorer.component.html",
    styleUrls: ["./type-explorer.component.scss"],
    providers: [TypesService]
})
export class TypeExplorerComponent implements OnInit {
    schema: Schema;
    searchInput: FormControl;

    members: SchemaMember[] = [];
    selectedMember: SchemaMember;

    constructor(
        private _elementRef: ElementRef,
        private service: TypesService
    ) {
        this.searchInput = new FormControl('');
    }

    onMemberSelected(member: SchemaMember) {
        this.selectedMember = member;
    }

    // Utility function for iterating keys in an object in an *ngFor
    objectKeys = Object.keys;

    attributes(member: SchemaMember): string[] {
        if (member.kind == SchemaMemberType.TYPE) {
            return Object.keys((member.member as Type).attributes)
        } else {
            return []
        }
    }

    ngOnInit() {
        this.service.getTypes().subscribe(
            schema => {
                let typeMembers: SchemaMember[] = schema.types.map((t) => SchemaMember.fromType(t as Type));
                let operationMembers: SchemaMember[] = [];
                schema.services.forEach((service) => operationMembers = operationMembers.concat(SchemaMember.fromService(service as Service)));
                let members: SchemaMember[] = typeMembers.concat(operationMembers);
                members = _.sortBy(members, (m: SchemaMember) => {
                    return m.name.fullyQualifiedName
                });
                this.schema = schema;
                this.members = members
            },
            error => console.log("error : " + error)
        );
    }

    loadTypeLinks(member: SchemaMember) {
        console.log("Load type links for `${member}`")
    }
}

export class SchemaMember {
    constructor(
        public readonly name: QualifiedName,
        public readonly kind: SchemaMemberType,
        public readonly aliasForType: string,
        public readonly member: Type | Service | Operation,
        public readonly sources: SourceCode[]
    ) {
        this.attributeNames = kind == SchemaMemberType.TYPE
            ? Object.keys((member as Type).attributes)
            : [];

        let tags = [
            new SchemaTag(1, _.capitalize(kind.toString()), "red")
        ];
        if (this.aliasForType) {
            tags.push(new SchemaTag(2, `Alias: ${this.aliasForType}`, "blue"))
        }
        this.tags = tags;
    }

    attributeNames: string[];

    readonly tags: SchemaTag[];

    static fromService(service: Service): SchemaMember[] {
        return service.operations.map(operation => {
            return new SchemaMember(
                {name: operation.name, fullyQualifiedName: service.name.fullyQualifiedName + " #" + operation.name},
                SchemaMemberType.OPERATION,
                null,
                operation,
                [] // TODO
            )
        })

    }

    static fromType(type: Type): SchemaMember {
        return new SchemaMember(
            type.name,
            SchemaMemberType.TYPE,
            (type.aliasForType) ? type.aliasForType.fullyQualifiedName : null,
            type,
            type.sources
        )
    }
}

export enum SchemaMemberType {
    SERVICE = "SERVICE",
    TYPE = "TYPE",
    OPERATION = "OPERATION"
}

export class SchemaTag {
    constructor(
        public readonly id: number,
        public readonly label: string,
        public readonly color: string
    ) {
    }

}