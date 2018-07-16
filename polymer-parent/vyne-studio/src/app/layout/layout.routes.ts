import { Routes, RouterModule } from "@angular/router";
import { LayoutComponent } from "./layout.component";
const LAYOUT_ROUTES: Routes = [
	{
		path: "",
		component: LayoutComponent,
		children: [
			{ path: "", redirectTo: "type-explorer", pathMatch: "full" },
			//---------------------------------------------------------->
			//Dashboards
			//---------------------------------------------------------->
			{
				path: "dashboards",
				loadChildren: "../pages/dashboards/dashboards.module#DashboardsModule"
			},
         {
            path: "type-explorer",
            loadChildren: "../type-explorer/type-explorer.module#TypeExplorerModule"
         },
         {
            path: "schema-editor",
            loadChildren: "../schema-editor/schema-editor.module#SchemaEditorModule"
         },
         {
            path: "query-editor",
            loadChildren: "../query-editor/query-editor.module#QueryEditorModule"
         },



         //---------------------------------------------------------->
			//Page Layouts
			//---------------------------------------------------------->
			{
				path: "page-layouts/full-width-v1",
				loadChildren:
					"../pages/page-layouts/full-width-v1/full-width-v1.module#FullWidthV1Module"
			},
			{
				path: "page-layouts/full-width-v2",
				loadChildren:
					"../pages/page-layouts/full-width-v2/full-width-v2.module#FullWidthV2Module"
			},
			{
				path: "page-layouts/boxed-layout-v1",
				loadChildren:
					"../pages/page-layouts/boxed-layout-v1/boxed-layout-v1.module#BoxedV1Module"
			},
			{
				path: "page-layouts/boxed-layout-v2",
				loadChildren:
					"../pages/page-layouts/boxed-layout-v2/boxed-layout-v2.module#BoxedV2Module"
			},
			{
				path: "page-layouts/detached-toolbar-left",
				loadChildren:
					"../pages/page-layouts/detached-toolbar-left/detached-toolbar-left.module#DetachedToolbarLeftModule"
			},
			{
				path: "page-layouts/detached-toolbar-right",
				loadChildren:
					"../pages/page-layouts/detached-toolbar-right/detached-toolbar-right.module#DetachedToolbarRightModule"
			},
			{
				path: "page-layouts/left-side-nav-v1",
				loadChildren:
					"../pages/page-layouts/left-side-nav-v1/left-side-nav-v1.module#LeftSideNavV1Module"
			},
			{
				path: "page-layouts/left-side-nav-v2",
				loadChildren:
					"../pages/page-layouts/left-side-nav-v2/left-side-nav-v2.module#LeftSideNavV2Module"
			},
			{
				path: "page-layouts/right-side-nav-v1",
				loadChildren:
					"../pages/page-layouts/right-side-nav-v1/right-side-nav-v1.module#RightSideNavV1Module"
			},
			{
				path: "page-layouts/right-side-nav-v2",
				loadChildren:
					"../pages/page-layouts/right-side-nav-v2/right-side-nav-v2.module#RightSideNavV2Module"
			}
		]
	},

	// 404 Page Not Found
	{ path: "**", redirectTo: "dashboards" }
];

export const LayoutRoutes = RouterModule.forChild(LAYOUT_ROUTES);
