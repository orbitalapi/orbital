import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { QueryPanelComponent } from 'src/app/query-panel/query-panel.component';
import { QueryEditorComponent } from 'src/app/query-panel/query-editor/query-editor.component';
import { QueryPanelModule } from 'src/app/query-panel/query-panel.module';
import { UiCustomisations } from '../../environments/ui-customisations';

@NgModule({
  imports: [
    QueryPanelModule,
    RouterModule.forChild([
      {
        path: '', component: QueryPanelComponent, children: [
          {
            path: 'editor', component: QueryEditorComponent, title: `${UiCustomisations.productName}: Query editor`
          }
        ]
      }
    ])
  ],
})
export class QueryPanelRouteModule {
}
