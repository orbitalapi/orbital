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

## Contiuining efforts
Getting Storybook and Angular 10 playing nicely has been impossible:
 * Storybook 6 is using webpack 5, but Angular 10 is on Webpack 4
 * Can't use Storybook 5 with Angular 10, because of Typescript version incompatibility.
 * This leads to LOTS of problems getting styles loading.
 * Lots of the libraries that Storbyook is using have moved onto WP5, with workarounds for WP4.
 * This has lead to lots of blind copy & paste of webpack code.
 * The major blocker was ngx-graph, and there's a merged (unreleased pr) for Angular12 support, so going to try to go to Ng12