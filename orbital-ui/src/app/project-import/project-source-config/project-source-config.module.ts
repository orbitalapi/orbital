import {NgModule} from '@angular/core';
import {FileConfigComponent} from 'src/app/project-import/project-source-config/file-config.component';
import {GitConfigComponent} from 'src/app/project-import/project-source-config/git-config.component';
import {
  TuiCheckboxModule,
  TuiComboBoxModule, TuiFieldErrorPipeModule,
  TuiInputFilesModule,
  TuiInputModule,
  TuiSelectModule
} from '@taiga-ui/kit';
import {
  TuiButtonModule,
  TuiDataListModule, TuiErrorModule,
  TuiGroupModule, TuiHintModule,
  TuiLoaderModule,
  TuiNotificationModule
} from '@taiga-ui/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';
import {OpenApiPackageConfigComponent} from './open-api-package-config.component';
import {ValidIdentifierDirective} from './valid-identifier.directive';
import {SemverValidatorDirective} from './semver-validator.directive';
import {AvroPackageConfigComponent} from "./avro-package-config.component";
import {PackageIdentifierInputComponent} from "../../package-identifier-input/package-identifier-input.component";


@NgModule({
  imports: [
    TuiInputModule,
    TuiNotificationModule,
    FormsModule,
    TuiButtonModule,
    CommonModule,
    TuiComboBoxModule,
    TuiDataListModule,
    TuiSelectModule,
    TuiCheckboxModule,
    TuiGroupModule,
    TuiInputFilesModule,
    TuiLoaderModule,
    AvroPackageConfigComponent,
    PackageIdentifierInputComponent,
    ValidIdentifierDirective,
    SemverValidatorDirective,
    TuiHintModule,
    TuiFieldErrorPipeModule,
    ReactiveFormsModule,
    TuiErrorModule
  ],
  exports: [FileConfigComponent, GitConfigComponent],
  declarations: [FileConfigComponent, GitConfigComponent, OpenApiPackageConfigComponent],
  providers: [],
})
export class ProjectSourceConfigModule {
}
