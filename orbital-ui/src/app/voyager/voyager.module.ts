import { TuiTextfieldControllerModule, TuiInputModule, TuiInputCopyModule, TuiSelectModule } from "@taiga-ui/legacy";
import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {PlaygroundToolbarComponent} from 'src/app/voyager/toolbar/playground-toolbar.component';
import {MatIconModule, MatIconRegistry} from '@angular/material/icon';
import {DomSanitizer} from '@angular/platform-browser';
import {MatButtonModule} from '@angular/material/button';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {SubscribeDialogComponent} from './subscribe-dialog/subscribe-dialog.component';
import { TuiDataListWrapper, TuiCheckbox } from '@taiga-ui/kit';
import {TuiLabel, TuiDropdown, TuiLink, TuiDialog, TuiButton, TuiHint, TuiAppearance} from '@taiga-ui/core';
import {ShareDialogComponent} from './share-dialog/share-dialog.component';
import {RouterLink, RouterLinkActive} from '@angular/router';


@NgModule({
    declarations: [
        PlaygroundToolbarComponent,
        SubscribeDialogComponent,
        ShareDialogComponent
    ],
    exports: [
        PlaygroundToolbarComponent
    ],
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    TuiDialog,
    TuiInputModule,
    ReactiveFormsModule,
    TuiSelectModule,
    TuiButton,
    TuiTextfieldControllerModule,
    ...TuiDataListWrapper,
    TuiLabel,
    ...TuiHint,
    TuiLink,
    FormsModule,
    TuiInputCopyModule,
    ...TuiDropdown,
    RouterLink,
    RouterLinkActive,
    TuiCheckbox,
    TuiAppearance,
  ],
})
export class VoyagerModule {

  constructor(private matIconRegistry: MatIconRegistry, private domSanitizer: DomSanitizer) {
    this.matIconRegistry.addSvgIcon(
      `brandGitHub`,
      this.domSanitizer.bypassSecurityTrustResourceUrl(`../../assets/img/tabler/brand-github.svg`)
    );
    this.matIconRegistry.addSvgIcon(
      `slack`,
      this.domSanitizer.bypassSecurityTrustResourceUrl(`../../assets/img/slack.svg`)
    );
    this.matIconRegistry.addSvgIcon(
      `orbital`,
      this.domSanitizer.bypassSecurityTrustResourceUrl(`../../assets/img/orbital_logo_white.svg`)
    );
    this.matIconRegistry.addSvgIcon(
      `share`,
      this.domSanitizer.bypassSecurityTrustResourceUrl(`../../assets/img/tabler/share.svg`)
    );
    this.matIconRegistry.addSvgIcon(
      `route-square-2`,
      this.domSanitizer.bypassSecurityTrustResourceUrl(`../../assets/img/tabler/route-square-2.svg`)
    );
    this.matIconRegistry.addSvgIcon(
      `stack-2`,
      this.domSanitizer.bypassSecurityTrustResourceUrl(`../../assets/img/tabler/stack-2.svg`)
    );
    this.matIconRegistry.addSvgIcon(
      `file-search`,
      this.domSanitizer.bypassSecurityTrustResourceUrl(`../../assets/img/tabler/file-search.svg`)
    );
    this.matIconRegistry.addSvgIcon(
      `trash`,
      this.domSanitizer.bypassSecurityTrustResourceUrl(`../../assets/img/tabler/trash.svg`)
    );
    this.matIconRegistry.addSvgIcon(
      `code-circle`,
      this.domSanitizer.bypassSecurityTrustResourceUrl(`../../assets/img/tabler/code-circle.svg`)
    );
    this.matIconRegistry.addSvgIcon(
      `book`,
      this.domSanitizer.bypassSecurityTrustResourceUrl(`../../assets/img/tabler/book.svg`)
    );
  }
}

