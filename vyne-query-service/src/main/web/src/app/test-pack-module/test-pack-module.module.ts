import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TestSpecFormComponent} from './test-spec-form.component';
import {MatLegacyFormFieldModule as MatFormFieldModule} from '@angular/material/legacy-form-field';
import {MatLegacyInputModule as MatInputModule} from '@angular/material/legacy-input';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatLegacyButtonModule as MatButtonModule} from '@angular/material/legacy-button';
import {ConfigDisabledFormComponent} from './config-disabled-form.component';
import {ConfigPersistResultsDisabledFormComponent} from './config-persist-results-disabled-form.component';
import { MatToolbarModule } from '@angular/material/toolbar';


@NgModule({
    declarations: [
        TestSpecFormComponent,
        ConfigDisabledFormComponent,
        ConfigPersistResultsDisabledFormComponent
    ],
    imports: [
        CommonModule,
        MatFormFieldModule,
        MatInputModule,
        FormsModule,
        ReactiveFormsModule,
        MatButtonModule,
        MatToolbarModule
    ]
})
export class TestPackModuleModule {
}
