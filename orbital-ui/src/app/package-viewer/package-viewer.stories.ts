import { moduleMetadata } from "@storybook/angular";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { PackageViewerModule } from "./package-viewer.module";
import { PACKAGE_LIST } from "./package-list.data";

export default {
  title: "Package viewer",

  decorators: [
    moduleMetadata({
      declarations: [],
      imports: [
        CommonModule,
        BrowserModule,
        BrowserAnimationsModule,
        PackageViewerModule,
      ],
    }),
  ],
};

export const PackageList = () => {
  return {
    template: `<div style="padding: 40px">
<app-package-list [packages]="packages"></app-package-list>
    </div>`,
    props: {
      packages: PACKAGE_LIST,
    },
  };
};
