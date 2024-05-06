import { RouterModule } from '@angular/router';
import { AuthGuard } from 'src/app/services/auth.guard';
import { VynePrivileges } from 'src/app/services/user-info.service';
import { LandingPageContainerComponent } from 'src/app/landing-page/landing-page-container.component';
import { FeatureFlagGuard } from './services/feature-flag.guard';
import { UiCustomisations } from '../environments/ui-customisations';

export const APP_ROUTES = RouterModule.forRoot(
  [
    {
      path: '',
      component: LandingPageContainerComponent,
      title: `${UiCustomisations.productName}`
    },
    {
      path: 'onboarding', // working title for now...
      loadChildren: () => import('./onboarding/onboarding.route.module').then(m => m.OnboardingRouteModule),
      canActivate: [FeatureFlagGuard],
      data: {requiredFeatureFlag: 'onboardingEnabled'}
    },
    {
      path: 'dashboard',
      loadComponent: () => import('./dashboard/dashboard.component').then(m => m.DashboardComponent),
      canActivate: [FeatureFlagGuard],
      data: {requiredFeatureFlag: 'dashboardEnabled'}
    },
    {
      path: 'catalog',
      loadChildren: () => import('./data-catalog/data-catalog.module').then(m => m.DataCatalogModule),
      canActivate: [AuthGuard],
      data: {requiredAuthority: VynePrivileges.BrowseCatalog}
    },
    {
      path: 'services',
      loadChildren: () => import('./service-view/service-view.routes').then(m => m.serviceViewRoutes),
      canActivate: [AuthGuard],
      data: {requiredAuthority: VynePrivileges.BrowseCatalog},
      title: `${UiCustomisations.productName}: Services`
    },
    {
      path: 'query',
      loadChildren: () => import('./query-panel/query-panel.route.module').then(m => m.QueryPanelRouteModule),
      canActivate: [AuthGuard],
      data: {requiredAuthority: VynePrivileges.RunQuery}
    },
    {
      path: 'query-history',
      loadChildren: () => import('./query-history/query-history.module').then(m => m.QueryHistoryModule),
      canActivate: [AuthGuard],
      data: {requiredAuthority: VynePrivileges.ViewQueryHistory}
    },
    {
      path: 'designer',
      loadChildren: () => import('./model-designer/model-designer.module').then(m => m.ModelDesignerModule),
      canActivate: [AuthGuard],
      data: {requiredAuthority: VynePrivileges.EditSchema}
    },
    {
      path: 'projects',
      loadChildren: () => import('./project-explorer/project-explorer.module').then(m => m.ProjectExplorerModule),
      canActivate: [AuthGuard],
      data: {requiredAuthority: VynePrivileges.BrowseSchema},
    },
    {
      path: 'data-source-manager',
      loadChildren: () => import('./data-source-manager/data-source-manager.routes').then(m => m.dataSourceManagerRoutes),
      canActivate: [AuthGuard],
      data: {requiredAuthority: VynePrivileges.ViewConnections}
    },
    {
      path: 'authentication-manager',
      loadChildren: () => import('./auth-manager/auth-manager.module').then(m => m.AuthManagerModule),
      canActivate: [AuthGuard],
      data: {requiredAuthority: VynePrivileges.ViewAuthenticationTokens}
    },
    {
      path: 'workspace',
      loadChildren: () => import('./workspace-manager/workspace-manager.module').then(m => m.WorkspaceManagerModule)
    },
    {
      path: 'endpoints',
      loadChildren: () => import('./endpoint-manager/endpoint-manager.routes').then(m => m.endpointManagerRoutes)
    },
    {
      path: 'policies',
      canActivate: [FeatureFlagGuard],
      loadChildren: () => import('./policy-manager/policy-manager.routes').then(m => m.policyManagerRoutes),
      data: {requiredFeatureFlag: 'policiesEnabled'}
    },
    // Redirects for deprecated routes, should be able to remove these over time
    {
      path: 'schemas',
      redirectTo: 'projects'
    },
    {
      path: 'schema-importer',
      redirectTo: 'project-import'
    },
    {
      path: 'connection-manager',
      redirectTo: 'data-source-manager'
    },
    // Note: This HAS to be last in the list, and acts as a catch all
    {
      path: '**',
      redirectTo: '',
    },


    // Experiments / Deprecated:
    //     {
    //         path: 'cask-viewer',
    //         loadChildren: () => import('./cask-viewer/cask-viewer.module').then(m => m.CaskViewerModule),
    //         canActivate: [AuthGuard],
    //         data: {requiredAuthority: VynePrivileges.ViewCaskDefinitions}
    //     },
    // {
    //         path: 'pipelines',
    //         loadChildren: () => import('./pipelines/pipelines.module').then(m => m.PipelinesModule)
    //     },
    //   {
    //         path: 'data-explorer',
    //         loadChildren: () => import('./data-explorer/data-explorer.route.module').then(m => m.DataExplorerRouteModule),
    //         canActivate: [AuthGuard],
    //         data: {requiredAuthority: VynePrivileges.EditSchema}
    //     },
    //     // {
    //     //   path: 'workbook',
    //     //   component: DataWorkbookContainerComponent,
    //     //   canActivate: [AuthGuard],
    //     //   data: { requiredAuthority: VynePrivileges.EditSchema }
    //     // },

  ],
  {
    useHash: false,
    anchorScrolling: 'enabled',
    scrollPositionRestoration: 'disabled'
  }
);
