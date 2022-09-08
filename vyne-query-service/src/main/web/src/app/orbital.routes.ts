import { RouterModule } from '@angular/router';
import { AuthGuard } from 'src/app/services/auth.guard';
import { VynePrivileges } from 'src/app/services/user-info.service';
import { LandingPageContainerComponent } from 'src/app/landing-page/landing-page-container.component';
import { QueryPanelRouteModule } from 'src/app/query-panel/query-panel.route.module';

export const ORBITAL_ROUTES = RouterModule.forRoot(
  [
    {
      path: '',
      component: LandingPageContainerComponent
    },
    {
      path: 'catalog',
      loadChildren: () => import('./data-catalog/data-catalog.module').then(m => m.DataCatalogModule),
      canActivate: [AuthGuard],
      data: { requiredAuthority: VynePrivileges.BrowseCatalog }
    },
    {
      path: 'services',
      loadChildren: () => import('./service-view/service-view.module').then(m => m.ServiceViewModule),
      canActivate: [AuthGuard],
      data: { requiredAuthority: VynePrivileges.BrowseCatalog }
    },
    {
      path: 'query',
      loadChildren: () => import('./query-panel/query-panel.route.module').then(m => m.QueryPanelRouteModule),
    },
    {
      path: 'data-explorer',
      loadChildren: () => import('./data-explorer/data-explorer.module').then(m => m.DataExplorerModule),
      canActivate: [AuthGuard],
      data: { requiredAuthority: VynePrivileges.EditSchema }
    },
    // {
    //   path: 'workbook',
    //   component: DataWorkbookContainerComponent,
    //   canActivate: [AuthGuard],
    //   data: { requiredAuthority: VynePrivileges.EditSchema }
    // },
    {
      path: 'schema-explorer',
      loadChildren: () => import('./schema-explorer/schema-explorer.module').then(m => m.SchemaExplorerModule),
      canActivate: [AuthGuard],
      data: { requiredAuthority: VynePrivileges.BrowseSchema },
    },
    {
      path: 'schema-importer',
      loadChildren: () => import('./schema-importer/schema-importer.module').then(m => m.SchemaImporterModule),
      canActivate: [AuthGuard],
      data: { requiredAuthority: VynePrivileges.EditSchema }
    },
    {
      path: 'query-history',
      loadChildren: () => import('./query-history/query-history.module').then(m => m.QueryHistoryModule),
      canActivate: [AuthGuard],
      data: { requiredAuthority: VynePrivileges.ViewQueryHistory }
    },
    {
      path: 'cask-viewer',
      loadChildren: () => import('./cask-viewer/cask-viewer.module').then(m => m.CaskViewerModule),
      canActivate: [AuthGuard],
      data: { requiredAuthority: VynePrivileges.ViewCaskDefinitions }
    },
    {
      path: 'connection-manager',
      loadChildren: () => import('./connection-manager/connection-manager.module').then(m => m.ConnectionManagerModule),
      canActivate: [AuthGuard],
      data: { requiredAuthority: VynePrivileges.ViewConnections }
    },
    {
      path: 'authentication-manager',
      loadChildren: () => import('./auth-mananger/auth-manager.module').then(m => m.AuthManagerModule),
      canActivate: [AuthGuard],
      data: { requiredAuthority: VynePrivileges.ViewAuthenticationTokens }
    },
    {
      path: 'pipeline-manager',
      loadChildren: () => import('./pipelines/pipelines.module').then(m => m.PipelinesModule)
    }
  ],
  {
    useHash: false,
    anchorScrolling: 'enabled',
    scrollPositionRestoration: 'disabled',
    relativeLinkResolution: 'legacy'
  }
);
