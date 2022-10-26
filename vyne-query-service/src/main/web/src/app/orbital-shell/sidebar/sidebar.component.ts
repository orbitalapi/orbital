import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AppInfoService } from 'src/app/services/app-info.service';
import { Router } from '@angular/router';
import { VynePrivileges } from 'src/app/services/user-info.service';

@Component({
  selector: 'app-sidebar',
  template: `
    <nav aria-label="Sidebar" class="hidden md:block md:flex-shrink-0 md:overflow-y-auto bg-midnight h-full">
      <div class="relative flex items-center w-28 flex-col space-y-3 p-3 pt-6">
        <a *ngFor="let element of sidebarElements"
           [routerLink]="element.route"
           [tuiHint]="element.title"
           tuiHintDirection="right"
           class="bg-midnight text-white flex-shrink-0 inline-flex items-center justify-center h-14 w-14 rounded-lg mb-8">
          <div class="flex items-center flex-col">
            <tui-svg [src]="element.icon" class="h-8 w-auto mb-2"></tui-svg>
            <span>{{element.title}}</span>
          </div>

        </a>
      </div>
    </nav>
  `,
  styleUrls: ['./sidebar.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SidebarComponent {

  readonly sidebarElements: SidebarElement[] = [
    {
      title: 'Catalog',
      icon: '../assets/img/iconpark-icons/book-open.svg',
      route: 'catalog',
      requiredAuthority: VynePrivileges.BrowseCatalog
    },
    {
      title: 'Schemas',
      icon: 'assets/img/iconpark-icons/file-code.svg',
      route: 'schemas',
      requiredAuthority: VynePrivileges.BrowseSchema
    },
  ];

  constructor(private appInfoService: AppInfoService,
              private router: Router) {
  }

}

export interface SidebarElement {
  title: string;
  icon: string;
  iconActive?: string;
  route: string;
  requiredAuthority: VynePrivileges;
}
