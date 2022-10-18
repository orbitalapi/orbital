import { RouterModule } from '@angular/router';
import { OrbitalShellComponent } from 'src/app/orbital-shell/orbital-shell.component';

export const ORBITAL_ROUTES = RouterModule.forRoot([
  {
    path: '',
    component: OrbitalShellComponent
  }
]);
