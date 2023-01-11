import { Component } from '@angular/core';
import { UserInfoService } from 'src/app/services/user-info.service';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs/internal/Observable';

@Component({
  selector: 'app-navbar',
  styleUrls: ['./navbar.component.scss'],
  template: `
    <div class="md:flex md:min-w-0 md:flex-1 md:items-center md:justify-between">
      <div class="min-w-0 flex-1">
        <div class="relative max-w-2xl text-gray-400 focus-within:text-gray-500">
          <input id="desktop-search" type="search" placeholder="Search"
                 class="block w-full border-transparent pl-12 placeholder-gray-500 focus:border-transparent focus:ring-0 sm:text-sm">
          <div class="pointer-events-none absolute inset-y-0 left-0 flex items-center justify-center pl-4">
            <!-- Heroicon name: mini/magnifying-glass -->
            <svg class="h-5 w-5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor"
                 aria-hidden="true">
              <path fill-rule="evenodd"
                    d="M9 3.5a5.5 5.5 0 100 11 5.5 5.5 0 000-11zM2 9a7 7 0 1112.452 4.391l3.328 3.329a.75.75 0 11-1.06 1.06l-3.329-3.328A7 7 0 012 9z"
                    clip-rule="evenodd"/>
            </svg>
          </div>
        </div>
      </div>
      <div class="ml-10 flex flex-shrink-0 items-center space-x-10 pr-4">
        <div class="relative inline-block text-left">
          <button type="button"
                  class="flex rounded-full bg-white text-sm focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2"
                  id="menu-0-button" aria-expanded="false" aria-haspopup="true">
            <span class="sr-only">Open user menu</span>
            <img class="h-8 w-8 rounded-full"
                 [src]="userAvatar$ | async"
                 alt="">
          </button>
        </div>
      </div>
    </div>
  `
})
export class NavbarComponent {
  userAvatar$: Observable<String>;

  constructor(private userInfoService: UserInfoService) {
    this.userAvatar$ = userInfoService.userInfo$
      .pipe(map(user => user.profileUrl));
  }


}
