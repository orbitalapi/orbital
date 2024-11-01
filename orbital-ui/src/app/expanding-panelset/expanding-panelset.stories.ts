import { TuiButton } from "@taiga-ui/core";
import { moduleMetadata } from "@storybook/angular";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { ExpandingPanelSetModule } from "./expanding-panel-set.module";
export default {
  title: "Expanding panelset",

  decorators: [
    moduleMetadata({
      declarations: [],
      imports: [
        CommonModule,
        TuiButton,
        BrowserModule,
        BrowserAnimationsModule,
        ExpandingPanelSetModule,
      ],
    }),
  ],
};

export const Header = () => {
  return {
    template: `<div style="padding: 40px">
<app-panel-header title="Code">
    <button tuiButton size="s">Run</button>
</app-panel-header>
    </div>`,
    props: {},
  };
};

Header.story = {
  name: "header",
};

export const Panelset = () => {
  return {
    template: `<div style="padding: 40px">
<app-panelset style="width: 400px;" >
<app-panel title="Catalog" icon="assets/img/tabler/vocabulary.svg">
<div>Hello, from Catalog</div>

</app-panel>
<app-panel title="History" icon="assets/img/tabler/history.svg">
<div>Hello, from History</div>

</app-panel>
</app-panelset>
    </div>`,
    props: {},
  };
};

Panelset.story = {
  name: "panelset",
};
