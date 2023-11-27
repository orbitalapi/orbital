
import '@angular/localize/init';

/**
 * By default, zone.js will patch all possible macroTask and DomEvents
 * user can disable parts of macroTask/DomEvents patch by setting following flags
 */

 // (window as any).__Zone_disable_requestAnimationFrame = true; // disable patch requestAnimationFrame
 // (window as any).__Zone_disable_on_property = true; // disable patch onProperty such as onclick
 // (window as any).__zone_symbol__BLACK_LISTED_EVENTS = ['scroll', 'mousemove']; // disable patch specified eventNames

 /*
 * in IE/Edge developer tools, the addEventListener will also be wrapped by zone.js
 * with the following flag, it will bypass `zone.js` patch for IE/Edge
 */
// (window as any).__Zone_enable_cross_context_check = true;

/***************************************************************************************************
 * Zone JS is required by default for Angular itself.
 */
import 'zone.js';  // Included with Angular CLI.


// https://stackoverflow.com/a/51232137/59015
// Note, these became required when we added a dependency to Slate for the wiki style editing
// If we migrate from Slate, or these become unrequired by slate, then remove them here.
import * as process from 'process';
window['process'] = process;
window['global'] = (window as any);
/***************************************************************************************************
 * APPLICATION IMPORTS
 */
