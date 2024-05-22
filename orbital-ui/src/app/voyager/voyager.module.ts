import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {PlaygroundToolbarComponent} from 'src/app/voyager/toolbar/playground-toolbar.component';
import {MatIconModule, MatIconRegistry} from '@angular/material/icon';
import {DomSanitizer} from '@angular/platform-browser';
import {MatButtonModule} from '@angular/material/button';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {SubscribeDialogComponent} from './subscribe-dialog/subscribe-dialog.component';
import {
  TuiCheckboxLabeledModule,
  TuiDataListWrapperModule,
  TuiInputCopyModule,
  TuiInputModule,
  TuiSelectModule
} from '@taiga-ui/kit';
import {
  TuiButtonModule,
  TuiDialogModule,
  TuiHintModule, TuiHostedDropdownModule,
  TuiLinkModule,
  TuiTextfieldControllerModule
} from '@taiga-ui/core';
import {ShareDialogComponent} from './share-dialog/share-dialog.component';
import {RouterLink} from "@angular/router";


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
    TuiDialogModule,
    TuiInputModule,
    ReactiveFormsModule,
    TuiSelectModule,
    TuiButtonModule,
    TuiTextfieldControllerModule,
    TuiDataListWrapperModule,
    TuiCheckboxLabeledModule,
    TuiHintModule,
    TuiLinkModule,
    FormsModule,
    TuiInputCopyModule,
    TuiHostedDropdownModule,
    RouterLink,
  ]
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
      `file-search`,
      this.domSanitizer.bypassSecurityTrustResourceUrl(`../../assets/img/tabler/file-search.svg`)
    );
    this.matIconRegistry.addSvgIcon(
      `trash`,
      this.domSanitizer.bypassSecurityTrustResourceUrl(`../../assets/img/tabler/trash.svg`)
    );
  }
}

