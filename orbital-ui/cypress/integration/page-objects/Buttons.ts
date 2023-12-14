// homepage buttons
export const dataCatalogButton = ':nth-child(1) > a' // data-e2e-id="data-catalog-sidebar"
export const schemaExplorerButton = ':nth-child(2) > a'// data-e2e-id="schema-explorer-sidebar"
export const queryBuilderButton = ':nth-child(3) > a'// data-e2e-id="query-builder-sidebar"
export const dataExplorerButton = ':nth-child(4) > a'// data-e2e-id="data-explorer-sidebar"
export const queryHistoryButton = ':nth-child(5) > a'// data-e2e-id="query-history-sidebar"
export const caskButton = ':nth-child(6) > a'// data-e2e-id="cask-sidebar"
// query builder buttons
export const findAsArray: string = '[class= "mat-checkbox mat-accent ng-untouched ng-pristine ng-valid"]'
export const submitQueryButton: string = '[class="mat-flat-button mat-primary"]'
export const operationListButton: string = '[value="stats"]'
export const callsButton: string = '[value="sequence"]'
export const gridView: string = '[value="table"]'
export const objectView: string = '[value="tree"]'
// query editor  buttons
export const runButton: string = '[class="mat-raised-button mat-accent ng-star-inserted"]'
export const runnerProgressBar: string = '[class = "progress ng-star-inserted"]'
export const runnerCancelButton: string = '.running-timer > .mat-stroked-button > .mat-button-wrapper'
export const downloadButton: string = '[class="downloadFileButton mat-stroked-button ng-star-inserted"]'
export const lineageCloseButton: string = 'span[class="clear-button"]'
export const runnerTimer : string = '[class="running-timer ng-star-inserted"]'
export const callsTabCloseButton: string = '[class= "close-button mat-icon-button"]'// profiler/Calls
// cask buttons
export const caskPanelItem: string = '[class="cask-config-row ng-star-inserted"]'
export const caskPanel: string = '[id = "mat-expansion-panel-header-0"]'
export const errorSearch: string = '.search-ingestion-errors-button'
// query history
export const queryCancelButton: string = '.timestamp-row > :nth-child(4)'
// data catalog
export const catalogFilter: string = '[class="title-bar"]'
export const entrType: string = '[id="mat-select-0"]'// data-e2e-id="entry-type"
export const type: string = '[value="TYPE"]'// data-e2e-id="type"
export const operation: string = '[value="OPERATION"]'// data-e2e-id="operation"
export const service: string = '[value="SERVICE"]'// data-e2e-id="service"
export const namespace: string = '[id="mat-input-0"]'// data-e2e-id="namespace"
export const name: string = '[id="mat-input-1"]'// data-e2e-id="name"
export const entryTypeArrow: string = '[class="mat-select-arrow-wrapper"]'
