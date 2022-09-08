import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { QueryPanelComponent } from 'src/app/query-panel/query-panel.component';
import { QueryEditorComponent } from 'src/app/query-panel/query-editor/query-editor.component';
import { QueryPanelModule } from 'src/app/query-panel/query-panel.module';

@NgModule({
  imports: [
    QueryPanelModule,
    RouterModule.forChild([
      {
        path: '', component: QueryPanelComponent, children: [
          {
            path: 'editor', component: QueryEditorComponent
          }
        ]
      }
    ])
  ],
})
export class QueryPanelRouteModule {
}
