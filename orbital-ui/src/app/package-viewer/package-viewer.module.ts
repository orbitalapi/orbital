import { TuiBadge } from "@taiga-ui/kit";
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TuiDataList, TuiIcon } from '@taiga-ui/core';
import {DropdownComponent} from '../query-panel/query-editor/query-editor-toolbar/dropdown/dropdown.component';
import { PackageListComponent } from './package-list/package-list.component';

@NgModule({
  declarations: [
    PackageListComponent
  ],
  exports: [
    PackageListComponent
  ],
  imports: [
    CommonModule,
    TuiBadge,
    RouterLinkActive,
    RouterLink,
    DropdownComponent,
    ...TuiDataList,
    TuiIcon
  ]
})
export class PackageViewerModule {
}
