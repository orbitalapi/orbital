import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {VoyagerToolbarComponent} from 'src/app/voyager/toolbar/voyager-toolbar.component';
import {MatIconModule, MatIconRegistry} from '@angular/material/icon';
import {DomSanitizer} from '@angular/platform-browser';
import {MatLegacyButtonModule as MatButtonModule} from '@angular/material/legacy-button';
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
  TuiHintModule,
  TuiLinkModule,
  TuiTextfieldControllerModule
} from '@taiga-ui/core';
import {ShareDialogComponent} from './share-dialog/share-dialog.component';


@NgModule({
    declarations: [
        VoyagerToolbarComponent,
        SubscribeDialogComponent,
        ShareDialogComponent
    ],
    exports: [
        VoyagerToolbarComponent
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
        TuiInputCopyModule
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
      `vyneDots`,
      this.domSanitizer.bypassSecurityTrustResourceUrl(`../../assets/img/vyne-logo-dots.svg`)
    );
    this.matIconRegistry.addSvgIcon(
      `orbital`,
      this.domSanitizer.bypassSecurityTrustResourceUrl(`../../assets/img/orbital_logo_white.svg`)
    );
    this.matIconRegistry.addSvgIcon(
      `share`,
      this.domSanitizer.bypassSecurityTrustResourceUrl(`../../assets/img/tabler/share.svg`)
    );
  }
}

