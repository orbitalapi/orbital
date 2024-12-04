import {TuiHintOverflow, TuiIcon} from '@taiga-ui/core';
import { TuiPagination } from "@taiga-ui/kit";
import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ChangelogListComponent} from './changelog-list.component';
import {MomentModule} from 'ngx-moment';
import {DiffEntryComponent} from './diff-entry.component';
import {DiffKindPipe} from './diff-kind.pipe';
import {TypeChangeComponent} from './type-change.component';
import {InputParamsChangeComponent} from './input-params-change.component';
import {MetadataChangeComponent} from './metadata-change.component';
import {DocumentationChangeComponent} from './documentation-change.component';
import {DiffListComponent} from './diff-list.component';

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
        InputParamsChangeComponent,
        DiffListComponent
    ],
  imports: [
    CommonModule,
    MomentModule,
    TuiIcon,
    TuiPagination,
    TuiHintOverflow,
  ],
})
export class ChangelogModule { }
