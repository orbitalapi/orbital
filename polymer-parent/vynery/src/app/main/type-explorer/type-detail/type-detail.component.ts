import {Component, Input, OnInit} from '@angular/core';
import {SchemaMember} from "../type-explorer.component";

@Component({
    selector: 'type-detail',
    templateUrl: './type-detail.component.html',
    styleUrls: ['./type-detail.component.scss']
})
export class TypeDetailComponent implements OnInit {

    constructor() {
    }

    ngOnInit() {
    }

    @Input()
    member: SchemaMember


}
