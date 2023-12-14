Although the build was coming out fine, there was a runtime error which was caused by a dependency(babel-plugin-styled-components) used by Rich-Markdown-Editor. 
In order to get the build work properly, some changes in angular.json file were necessary.
So, vendorChunk is assigned to true and buildOptimizer is assigned to false.

..vyne\vyne\vyne\vyne-query-service\src\main\web\angular.json

"configurations": {
            "production": {
              "fileReplacements": [
                {
                  "replace": "src/environments/environment.ts",
                  "with": "src/environments/environment.prod.ts"
                }
              ],
              "optimization": true,
              "outputHashing": "all",
              "sourceMap": false,
              "extractCss": true,
              "namedChunks": false,
              "aot": true, 
              "extractLicenses": true,
              "vendorChunk": <!-- false --> true,
              "buildOptimizer": <!-- true --> false
            },


These changes can be reverted whenever we update Typescript version above 3.7.5. It's simply because Rich-Markdown-Editor seems to be working well with Typescript 
after it's 10.0.0-17 version and such versions require mentioned Typescript versions.

For more information please see:
https://github.com/angular/angular-cli/issues/10655