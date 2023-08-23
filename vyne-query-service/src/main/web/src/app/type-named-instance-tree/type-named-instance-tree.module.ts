import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TypeNamedInstanceTreeComponent} from './type-named-instance-tree.component';
import {TuiTreeModule} from "@taiga-ui/kit";


@NgModule({
    declarations: [
        TypeNamedInstanceTreeComponent
    ],
    exports: [
        TypeNamedInstanceTreeComponent
    ],
    imports: [
        CommonModule,
        TuiTreeModule
    ]
})
export class TypeNamedInstanceTreeModule {
}
