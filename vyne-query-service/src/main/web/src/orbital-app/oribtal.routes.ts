import { RouterModule } from '@angular/router';
import { OrbitalShellComponent } from 'src/app/orbital-shell/orbital-shell.component';
import { OrbitalLandingPageComponent } from 'src/app/orbital-landing-page/orbital-landing-page.component';

export const ORBITAL_ROUTES = RouterModule.forRoot([
  {
    path: '',
    component: OrbitalShellComponent,
    children: [
      {
        path: '', component: OrbitalLandingPageComponent
      }
    ]
  }
]);
