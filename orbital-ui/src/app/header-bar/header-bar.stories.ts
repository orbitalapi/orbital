import { TuiRoot } from "@taiga-ui/core";
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {moduleMetadata} from '@storybook/angular';
import { CommonModule } from "@angular/common";
import {AuthService} from '../auth/auth.service';
import {Environment, ENVIRONMENT} from '../services/environment';
import { HeaderBarModule } from "./header-bar.module";
import { VyneUser } from "../services/user-info.service";

const userWithImage: VyneUser = {
  username: "Jimmy",
  email: "jimmy@vyne.co",
  name: "Jimmy Spitts",
  userId: "jimmy",
  profileUrl: "https://randomuser.me/api/portraits/women/68.jpg",
  grantedAuthorities: [],
  isAuthenticated: true,
  authenticationType: 'Oidc'
};
const userWithoutImage: VyneUser = {
  ...userWithImage,
  profileUrl: null,
};

export default {
  title: "User menu",

  decorators: [
    moduleMetadata({
      imports: [CommonModule, BrowserAnimationsModule, HeaderBarModule, TuiRoot],
      providers: [
        {
          provide: AuthService,
          useValue: {
            securityConfig: {
              accountManagementUrl: 'http://test.account-management.com',
              orgManagementUrl: 'http://test.org-management.com'
            }
          }
        },
        {
          provide: ENVIRONMENT,
          useValue: {
            serverUrl: "http://localhost:9022",
            production: false,
          } as Environment,
        }
      ]
    }),
  ],
};

export const UserWithImage = () => {
  return {
    template: `<tui-root style="padding: 40px">
    <app-avatar [user]="user"></app-avatar>
    </tui-root>`,
    props: {
      user: userWithImage,
    },
  };
};

UserWithImage.story = {
  name: "user with image",
};

export const UserWithoutImage = () => {
  return {
    template: `<tui-root style="padding: 40px">
    <app-avatar [user]="user"></app-avatar>
    </tui-root>`,
    props: {
      user: userWithoutImage,
    },
  };
};

UserWithoutImage.story = {
  name: "user without image",
};
