import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChangelogListComponent } from './changelog-list.component';
import { MomentModule } from 'ngx-moment';
import { DiffEntryComponent } from './diff-entry.component';
import { DiffKindPipe } from './diff-kind.pipe';
import { TuiSvgModule, TuiTableModeModule } from '@taiga-ui/core';
import { TypeChangeComponent } from './type-change.component';
import { InputParamsChangeComponent } from './input-params-change.component';
import { MetadataChangeComponent } from './metadata-change.component';



@NgModule({
  declarations: [
    ChangelogListComponent,
    DiffEntryComponent,
    DiffKindPipe,
    TypeChangeComponent,
    InputParamsChangeComponent,
    MetadataChangeComponent
  ],
  exports: [
    ChangelogListComponent
  ],
  imports: [
    CommonModule,
    MomentModule,
    TuiSvgModule,
  ]
})
export class ChangelogModule { }
