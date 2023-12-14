import {moduleMetadata, storiesOf} from '@storybook/angular';
import {AttributeTableComponent} from './attribute-table/attribute-table.component';
import {TypeViewerComponent} from './type-viewer.component';
import {APP_BASE_HREF, CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {RouterTestingModule} from '@angular/router/testing';
import {MatToolbarModule} from '@angular/material/toolbar';
import {TocHostDirective} from './toc-host.directive';
import {action} from '@storybook/addon-actions';

const actions = {
  contentsChanged: action('contentsChanged')
};

storiesOf('TableOfContents', module)
  .addDecorator(
    moduleMetadata({
      declarations: [TocHostDirective],
      imports: [CommonModule, BrowserModule]
    })
  ).add('default', () => {
  return {
    template: `<div appTocHost tocTag="h2" (contentsChanged)="onContentsChanged($event)">
    <h2>Chapter one</h2>
    <p>Prow scuttle parrel provost Sail ho shrouds spirits boom mizzenmast yardarm. Pinnace holystone mizzenmast quarter crow's nest nipperkin grog yardarm hempen halter furl. Swab barque interloper chantey doubloon starboard grog black jack gangway rutters.</p>
    <h2>Chapter two</h2>
    <p>Deadlights jack lad schooner scallywag dance the hempen jig carouser broadside cable strike colors. Bring a spring upon her cable holystone blow the man down spanker Shiver me timbers to go on account lookout wherry doubloon chase. Belay yo-ho-ho keelhaul squiffy black spot yardarm spyglass sheet transom heave to.</p>
    <h2>Chapter three</h2>
    <p>Trysail Sail ho Corsair red ensign hulk smartly boom jib rum gangway. Case shot Shiver me timbers gangplank crack Jennys tea cup ballast Blimey lee snow crow's nest rutters. Fluke jib scourge of the seven seas boatswain schooner gaff booty Jack Tar transom spirits.</p>
</div>`,
    props: {
      onContentsChanged: actions.contentsChanged
    }
  };
});

