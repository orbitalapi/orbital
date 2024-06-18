import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import {TuiDataListModule, TuiSvgModule} from '@taiga-ui/core';
import { TuiBadgeModule } from '@taiga-ui/kit';
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
    TuiBadgeModule,
    RouterLinkActive,
    RouterLink,
    DropdownComponent,
    TuiDataListModule,
    TuiSvgModule
  ]
})
export class PackageViewerModule {
}
