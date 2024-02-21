import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TuiBadgeModule } from '@taiga-ui/kit';
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
    RouterLink
  ]
})
export class PackageViewerModule {
}
