import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {AppComponent} from './app.component';
import {FormsModule} from '@angular/forms';
import {NgxGraphModule} from '@swimlane/ngx-graph';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {TypeLinkGraphModule} from './type-link-graph/type-link-graph.module';

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    NgxGraphModule,
    TypeLinkGraphModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule {
}
