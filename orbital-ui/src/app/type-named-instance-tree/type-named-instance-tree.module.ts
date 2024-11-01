import { TuiTree } from "@taiga-ui/kit";
import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TypeNamedInstanceTreeComponent} from './type-named-instance-tree.component';

@NgModule({
    declarations: [
        TypeNamedInstanceTreeComponent
    ],
    exports: [
        TypeNamedInstanceTreeComponent
    ],
    imports: [
        CommonModule,
        ...TuiTree
    ]
})
export class TypeNamedInstanceTreeModule {
}
