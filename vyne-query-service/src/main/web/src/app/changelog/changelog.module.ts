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
import { DocumentationChangeComponent } from './documentation-change.component';
import { DiffListComponent } from './diff-list.component';
import { TuiPaginationModule } from '@taiga-ui/kit';



@NgModule({
  declarations: [
    ChangelogListComponent,
    DiffEntryComponent,
    DiffKindPipe,
    TypeChangeComponent,
    InputParamsChangeComponent,
    MetadataChangeComponent,
    DocumentationChangeComponent,
    DiffListComponent
  ],
  exports: [
    ChangelogListComponent,
    InputParamsChangeComponent
  ],
    imports: [
        CommonModule,
        MomentModule,
        TuiSvgModule,
        TuiPaginationModule,
    ]
})
export class ChangelogModule { }
