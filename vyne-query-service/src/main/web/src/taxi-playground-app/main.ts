import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';

import { environment } from './environments/environment';
import { TaxiPlaygroundAppModule } from './taxi-playground-app.module';

if (environment.production) {
  enableProdMode();
}

platformBrowserDynamic().bootstrapModule(TaxiPlaygroundAppModule)
  .catch(err => console.log(err));
