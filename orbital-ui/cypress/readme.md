### Introduction
This directory contains end-to-end tests for **Vyne**.
It includes the test framework and the tests themselves.

The tests can be found in *vyne-query-service\src\main\web\cypress\integration\ui-tests*.

### Run Tests via Cypress
Use the following path to run tests in Cypress :

`notional\vyne\vyne-query-service\src\main\web`

You can run tests with following command :

`npm run automation`

### Configure Tests

##### Vyne App Url

You can configure `homePageUrl` from following path:

*cypress\integration\page-objects\Pages.ts*

##### Schema Type

Vyne Ui Tests using a specific schema type, which is _**smyrna.orders.Order**_ now.

You can configure `choosenSchema` from following path:

*cypress\integration\page-objects\Fields.ts*

##### File Uploads

Data Explorer tests upload file from following path:   
*cypress\fixtures*

### Screenshots and Videos

Cypress will automatically capture screenshots when a failure happens.

You can find `screenshots` in following path:

*cypress\screenshots*

Cypress records a video for each spec file when running tests.

You can find `videos` in following path:

*cypress\videos*