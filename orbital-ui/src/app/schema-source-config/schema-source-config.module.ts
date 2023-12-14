import { NgModule } from '@angular/core';
import { FileConfigComponent } from 'src/app/schema-source-config/file-config.component';
import { GitConfigComponent } from 'src/app/schema-source-config/git-config.component';
import {
  TuiCheckboxModule,
  TuiComboBoxModule,
  TuiInputFilesModule,
  TuiInputModule,
  TuiSelectModule
} from '@taiga-ui/kit';
import {
  TuiButtonModule,
  TuiDataListModule,
  TuiGroupModule,
  TuiLoaderModule,
  TuiNotificationModule
} from '@taiga-ui/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { OpenApiPackageConfigComponent } from './open-api-package-config.component';
import { ValidIdentifierDirective } from './valid-identifier.directive';
import { SemverValidatorDirective } from './semver-validator.directive';


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
    TuiLoaderModule
  ],
  exports: [FileConfigComponent, GitConfigComponent],
  declarations: [FileConfigComponent, GitConfigComponent, OpenApiPackageConfigComponent, ValidIdentifierDirective, SemverValidatorDirective],
  providers: [],
})
export class SchemaSourceConfigModule {
}
