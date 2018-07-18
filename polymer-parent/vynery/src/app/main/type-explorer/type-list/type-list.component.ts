import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {SchemaMember, SchemaMemberType} from "../type-explorer.component";
import {TypesService} from "../types.service";

@Component({
    selector: 'type-list',
    templateUrl: './type-list.component.html',
    styleUrls: ['./type-list.component.scss'],
    providers: [TypesService]
})
export class TypeListComponent implements OnInit {

    constructor(private typesService: TypesService) {
    }

    @Input()
    members: SchemaMember[] = [];

    @Input()
    term: string = "";

    @Output()
    memberSelected = new EventEmitter<SchemaMember>();


    // Can't seem to use enums in an ng-if.  Grrr.
    serviceType = SchemaMemberType.SERVICE;
    typeType = SchemaMemberType.TYPE;
    operationType = SchemaMemberType.OPERATION;

    ngOnInit() {
    }

    loadTypeLinks(member: SchemaMember) {
        console.log("Load type links for `${member}`")
    }

    onMemberSelected(member: SchemaMember) {
        this.memberSelected.next(member);
    }
}
