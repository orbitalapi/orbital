## Angular 10 & Node 14
This proejct is now built on Angular 10.
This requires Node 14.

Do not upgrade beyond Node14, or you'll hit compatability issues with node-sass 4.14.1
see:
https://www.npmjs.com/package/node-sass


## Upgrading Angular

This project has been upgraded to Angular 10.  At the time of writing, Angular 13 is the latest.

The blockers to move on further are:

## AgGrid / node-sass vs sass / Angular compiler -- reverting to es5
This project is pinned to Ag Grid 25.
v26 introduced a migration from node-sass to sass: https://github.com/ag-grid/ag-grid/commit/91747f44d0de217e66b063aeb8b30fe101a65764

That introduced an issue that we can only customize themes if we are using sass: https://github.com/ag-grid/ag-grid/issues/4642

Moving to sass causes the build to freeze with: 

```
Generating ES5 bundles for differential loading...
```

Despite several attempts which appeared to have resolved the issue, it kept re-appearing.
So, have reverted to `target: es5` in tsconfig.json.

This isn't ideal, but it seems that the only impact is a bigger build size. (for now)

Links: 

https://stackoverflow.com/questions/59531305/call-retries-were-exceeded-exception-while-ng-build

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
However, this is...

```
ModuleNotFoundError: Module not found: Error: Can't resolve 'src/app/services/query.service' in '/home/marty/dev/vyne/vyne-query-service/src/main/web/src/app/query-panel/query-editor'
at /home/marty/dev/vyne/vyne-query-service/src/main/web/node_modules/webpack/lib/Compilation.js:925:10
at /home/marty/dev/vyne/vyne-query-service/src/main/web/node_modules/webpack/lib/NormalModuleFactory.js:401:22
at /home/marty/dev/vyne/vyne-query-service/src/main/web/node_modules/webpack/lib/NormalModuleFactory.js:130:21
at /home/marty/dev/vyne/vyne-query-service/src/main/web/node_modules/webpack/lib/NormalModuleFactory.js:224:22
at /home/marty/dev/vyne/vyne-query-service/src/main/web/node_modules/neo-async/async.js:2830:7
at /home/marty/dev/vyne/vyne-query-service/src/main/web/node_modules/neo-async/async.js:6877:13
at /home/marty/dev/vyne/vyne-query-service/src/main/web/node_modules/webpack/lib/NormalModuleFactory.js:214:25
at /home/marty/dev/vyne/vyne-query-service/src/main/web/node_modules/webpack/node_modules/enhanced-resolve/lib/Resolver.js:213:14
at /home/marty/dev/vyne/vyne-query-service/src/main/web/node_modules/webpack/node_modules/enhanced-resolve/lib/Resolver.js:285:5
at eval (eval at create (/home/marty/dev/vyne/vyne-query-service/src/main/web/node_modules/tapable/lib/HookCodeFactory.js:33:10), <anonymous>:15:1)
at /home/marty/dev/vyne/vyne-query-service/src/main/web/node_modules/webpack/node_modules/enhanced-resolve/lib/UnsafeCachePlugin.js:44:7
at /home/marty/dev/vyne/vyne-query-service/src/main/web/node_modules/webpack/node_modules/enhanced-resolve/lib/Resolver.js:285:5
at eval (eval at create (/home/marty/dev/vyne/vyne-query-service/src/main/web/node_modules/tapable/lib/HookCodeFactory.js:33:10), <anonymous>:15:1)
at /home/marty/dev/vyne/vyne-query-service/src/main/web/node_modules/webpack/node_modules/enhanced-resolve/lib/Resolver.js:285:5
at eval (eval at create (/home/marty/dev/vyne/vyne-query-service/src/main/web/node_modules/tapable/lib/HookCodeFactory.js:33:10), <anonymous>:27:1)
at /home/marty/dev/vyne/vyne-query-service/src/main/web/node_modules/webpack/node_modules/enhanced-resolve/lib/DescriptionFilePlugin.js:67:43
resolve 'src/app/services/query.service' in '/home/marty/dev/vyne/vyne-query-service/src/main/web/src/app/query-panel/query-editor'
Parsed request is a module
using description file: /home/marty/dev/vyne/vyne-query-service/src/main/web/package.json (relative path: ./src/app/query-panel/query-editor)
Field 'browser' doesn't contain a valid alias configuration
resolve as module
/home/marty/dev/vyne/vyne-query-service/src/main/web/src/app/query-panel/query-editor/node_modules doesn't exist or is not a directory
/home/marty/dev/vyne/vyne-query-service/src/main/web/src/app/query-panel/node_modules doesn't exist or is not a directory
/home/marty/dev/vyne/vyne-query-service/src/main/web/src/app/node_modules doesn't exist or is not a directory
/home/marty/dev/vyne/vyne-query-service/src/main/web/src/node_modules doesn't exist or is not a directory
/home/marty/dev/vyne/vyne-query-service/src/main/node_modules doesn't exist or is not a directory
/home/marty/dev/vyne/vyne-query-service/src/node_modules doesn't exist or is not a directory
/home/marty/dev/vyne/vyne-query-service/node_modules doesn't exist or is not a directory
/home/marty/dev/vyne/node_modules doesn't exist or is not a directory
/home/marty/dev/node_modules doesn't exist or is not a directory
/home/marty/node_modules doesn't exist or is not a directory
/home/node_modules doesn't exist or is not a directory
/node_modules doesn't exist or is not a directory
```

This is fatal. I can't work out how to get past this.  It's not

## node version
Now need at least node 12.
