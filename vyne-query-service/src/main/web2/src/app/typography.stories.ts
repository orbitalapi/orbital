import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';

storiesOf('Typography', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, BrowserAnimationsModule]
    })
  )
  .add('default', () => {
    return {
      template: `<div style="padding: 40px; display: flex; ">
<div>
<h3>Headings</h3>
<h1>Heading 1</h1>
<h2>Heading 2</h2>
<h3>Heading 3</h3>
<h4>Heading 4</h4>
<h5>Heading 5</h5>
<h6>Heading 6</h6>
</div>
<hr>
  <div>
    <h3>Title styles</h3>
    <p><span class="subtitle">Subtitle</span></p>
    <p><span class="caption">Caption</span></p>
    <p><span class="caption-small">Caption Small</span></p>
  </div>
<hr>
  <div>
    <h3>Paragraph styles</h3>
    <p>Body text</p>
    <p style="font-weight: bold">Bold Body text</p>
    <p class="small">Body Small</p>
    <p class="small" style="font-weight: bold">Body Small and Bold</p>
    <p class="smaller">Body Smaller</p>
    <p class="smaller" style="font-weight: bold">Body Smaller and Bold</p>
  </div>
</div>`,
      props: {}
    };
  });
