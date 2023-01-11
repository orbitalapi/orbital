import { NgModule } from '@angular/core';
import { DataExplorerComponent } from './data-explorer.component';
import { RouterModule } from '@angular/router';
import { DataExplorerModule } from 'src/app/data-explorer/data-explorer.module';

@NgModule({
  imports: [
    DataExplorerModule,
    RouterModule.forChild([
      {
        path: '',
        component: DataExplorerComponent,
      },
    ])
  ],
  declarations: [],
  exports: [],
  entryComponents: []

})
export class DataExplorerRouteModule {
}
