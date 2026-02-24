import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ConfigDisabledFormComponent} from './config-disabled-form.component';
import {ConfigPersistResultsDisabledFormComponent} from './config-persist-results-disabled-form.component';
import { MatToolbarModule } from '@angular/material/toolbar';


@NgModule({
    declarations: [
        ConfigDisabledFormComponent,
        ConfigPersistResultsDisabledFormComponent
    ],
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        MatToolbarModule
    ]
})
export class TestPackModuleModule {
}
