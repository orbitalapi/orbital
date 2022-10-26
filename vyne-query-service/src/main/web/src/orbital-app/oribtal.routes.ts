import { RouterModule } from '@angular/router';
import { OrbitalShellComponent } from 'src/app/orbital-shell/orbital-shell.component';
import { OrbitalLandingPageComponent } from 'src/app/orbital-landing-page/orbital-landing-page.component';
import { AuthGuard } from 'src/app/services/auth.guard';
import { VynePrivileges } from 'src/app/services/user-info.service';

export const ORBITAL_ROUTES = RouterModule.forRoot([
  {
    path: '',
    component: OrbitalShellComponent,
    children: [
      {
        path: '', component: OrbitalLandingPageComponent
      },
      // {
      //   path: 'catalog',
      //   loadChildren: () => import('../app/data-catalog/data-catalog.module').then(m => m.DataCatalogModule),
      //   canActivate: [AuthGuard],
      //   data: { requiredAuthority: VynePrivileges.BrowseCatalog }
      // },

      {
        path: 'schemas',
        loadChildren: () => import('../app/schema-explorer/schema-explorer.module').then(m => m.SchemaExplorerModule),
        canActivate: [AuthGuard],
        data: { requiredAuthority: VynePrivileges.BrowseSchema },
      },
      {
        path: 'schema-importer',
        loadChildren: () => import('../app/schema-importer/schema-importer.module').then(m => m.SchemaImporterModule),
        canActivate: [AuthGuard],
        data: { requiredAuthority: VynePrivileges.EditSchema }
      },

    ]
  }
]);
