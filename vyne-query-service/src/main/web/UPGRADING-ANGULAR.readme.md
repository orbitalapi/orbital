## Upgrading Angular

This project has been upgraded to Angular 10.  At the time of writing, Angular 13 is the latest.

The blockers to move on further are:

## @swimlane/ngx-graph couldn't work with Angular 11.

 * ngx-graph 8.0.0.rc-1 has a mess of dependencies
 * They removed support for ngx-chart, which is required if you need tooltips (which we do)
 * There's a confict in depenency versions when adding ngx-chart back in - I gave up.  
 * More persistence might work here.

## storybook not working
Storybook has been upgraded to Storybook 6, as Storybook 5 is now incompatible with the
typescript version used in Angular 10.#

Storybook 6 seems to have a few issues:

```
WARN   Failed to load preset: {"name":"/home/marty/dev/vyne/vyne-query-service/src/main/web/node_modules/@storybook/angular/dist/ts3.9/server/framework-preset-angular-cli.js","type":"presets"} on level 1
ERR! Error: Cannot find module '@angular-devkit/build-angular/src/webpack/configs'
ERR! Require stack:
ERR! - /home/marty/dev/vyne/vyne-query-service/src/main/web/node_modules/@storybook/angular/dist/ts3.9/server/angular-cli-webpack-12.2.x.js
```
It's not clear if this is fatal.


```
ModuleNotFoundError: Module not found: Error: Can't resolve 'src/environments/environment' in '/home/marty/dev/vyne/vyne-query-service/src/main/web/src/app/services'
```

This is fatal. I can't work out how to get past this.

## node version
Now need at least node 12.
