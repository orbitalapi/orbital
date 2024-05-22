import { moduleMetadata } from "@storybook/angular";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { ObjectViewModule } from "../object-view/object-view.module";
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
      imports: [CommonModule, BrowserModule, HeaderBarModule],
    }),
  ],
};

export const UserWithImage = () => {
  return {
    template: `<div style="padding: 40px">
    <app-avatar [user]="user"></app-avatar>
    </div>`,
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
    template: `<div style="padding: 40px">
    <app-avatar [user]="user"></app-avatar>
    </div>`,
    props: {
      user: userWithoutImage,
    },
  };
};

UserWithoutImage.story = {
  name: "user without image",
};
