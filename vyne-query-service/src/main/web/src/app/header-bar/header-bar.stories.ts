import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {ObjectViewModule} from '../object-view/object-view.module';
import {HeaderBarModule} from './header-bar.module';
import {VyneUser} from '../services/user-info.service';

const userWithImage: VyneUser = {
  username: 'Jimmy',
  email: 'jimmy@vyne.co',
  name: 'Jimmy Spitts',
  userId: 'jimmy',
  profileUrl: 'https://randomuser.me/api/portraits/women/68.jpg',
  grantedAuthorities: [],
  isAuthenticated: true
};
const userWithoutImage: VyneUser = {
  ...userWithImage,
  profileUrl: null
};
storiesOf('User menu', module)
  .addDecorator(
    moduleMetadata({
      imports: [CommonModule, BrowserModule, HeaderBarModule]
    })
  )
  .add('user with image', () => {
    return {
      template: `<div style="padding: 40px">
    <app-avatar [user]="user"></app-avatar>
    </div>`,
      props: {
        user: userWithImage
      }
    };
  })
  .add('user without image', () => {
    return {
      template: `<div style="padding: 40px">
    <app-avatar [user]="user"></app-avatar>
    </div>`,
      props: {
        user: userWithoutImage
      }
    };
  });
