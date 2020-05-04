import { configure } from '@storybook/angular';
import  '!style-loader!css-loader!sass-loader!../src/styles.scss'
// automatically import all files ending in *.stories.ts
const req = require.context('../src/', true, /\.stories\.ts$/);

function loadStories() {
  req.keys().forEach(filename => req(filename));
}

configure(loadStories, module);
