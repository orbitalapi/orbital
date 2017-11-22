import { HashLocationStrategy, LocationStrategy } from "@angular/common";
import { BrowserModule, Title } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { NgModule } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { HttpModule } from "@angular/http";
import { AppComponent } from "./app.component";
import { AppState, InternalStateType } from "./app.service";
import { GlobalState } from "./app.state";
import { ServicesModule } from "./shared/services/services.module";
import { SharedModule } from "./shared/shared.module";
import { AppRoutingModule } from "./app.routing";
import { HttpClientModule } from '@angular/common/http';

// For spiking, remove these:
// import { InMemoryWebApiModule } from "angular-in-memory-web-api";
// import { MaterialLabDbService } from "./shared/data/MaterialLabDb.service";

import { QueryEditorModule } from './query-editor/query-editor.module'

import { HammerGestureConfig, HAMMER_GESTURE_CONFIG } from '@angular/platform-browser';
import { NavDropDownDirectives } from "app/shared/directives/nav-dropdown.directive";

export class HammerConfig extends HammerGestureConfig {
   overrides = <any>{
      'swipe': { velocity: 0.4, threshold: 20 }
   }
}
// Application wide providers
const APP_PROVIDERS = [
   AppState,
   GlobalState,
   Title,

   { provide: LocationStrategy, useClass: HashLocationStrategy },
   { provide: HAMMER_GESTURE_CONFIG, useClass: HammerConfig }
];

export type StoreType = {
   state: InternalStateType;
   restoreInputValues: () => void;
   disposeOldHosts: () => void;
};

@NgModule({
   declarations: [AppComponent, NavDropDownDirectives],
   imports: [
      BrowserModule,
      FormsModule,
      HttpModule,
      HttpClientModule,
      ReactiveFormsModule,
      BrowserAnimationsModule,
      ServicesModule,
      SharedModule.forRoot(),
      AppRoutingModule,
      // InMemoryWebApiModule.forRoot(MaterialLabDbService)
   ],
   providers: [APP_PROVIDERS],
   bootstrap: [AppComponent]
})
export class AppModule {
   constructor(public appState: AppState) { }
}
