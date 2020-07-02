import {NgModule} from '@angular/core';
import {ResultContainerComponent} from './result-container.component';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import {MatTabsModule} from '@angular/material/tabs';
import {ObjectViewModule} from '../../object-view/object-view.module';
import {VyneServicesModule} from '../../services/vyne-services.module';
import {SimpleCodeViewerModule} from '../../simple-code-viewer/simple-code-viewer.module';
import {CallExplorerModule} from '../taxi-viewer/call-explorer/call-explorer.module';
import {MatSidenavModule} from '@angular/material/sidenav';
import {TypedInstancePanelModule} from '../../typed-instance-panel/typed-instance-panel.module';


@NgModule({
  imports: [BrowserModule, CommonModule, MatTabsModule,
    ObjectViewModule, VyneServicesModule,
    SimpleCodeViewerModule,
    CallExplorerModule
  ],
  exports: [ResultContainerComponent],
  declarations: [ResultContainerComponent],
  providers: [],
})
export class ResultViewerModule {
}
