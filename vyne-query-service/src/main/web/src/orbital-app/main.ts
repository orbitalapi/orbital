import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';

import { environment } from './environments/environment';
import { OrbitalAppModule } from 'src/orbital-app/orbital-app.module';

if (environment.production) {
  enableProdMode();
}

platformBrowserDynamic().bootstrapModule(OrbitalAppModule)
  .catch(err => console.log(err));
