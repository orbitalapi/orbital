// Angular
// https://angular.io/
import { NgModule, ModuleWithProviders } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { RouterModule } from "@angular/router";
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { FlexLayoutModule } from '@angular/flex-layout';
// Angular Material
// https://material.angular.io/
import {
	MatAutocompleteModule,
	MatButtonModule,
	MatButtonToggleModule,
	MatCardModule,
	MatCheckboxModule,
	MatChipsModule,
	MatDatepickerModule,
	MatDialogModule,
	MatExpansionModule,
	MatGridListModule,
	MatIconModule,
	MatInputModule,
	MatListModule,
	MatMenuModule,
	MatNativeDateModule,
	MatProgressBarModule,
	MatProgressSpinnerModule,
	MatRadioModule,
	MatRippleModule,
	MatSelectModule,
	MatSidenavModule,
	MatSliderModule,
	MatSlideToggleModule,
	MatSnackBarModule,
	MatTabsModule,
	MatToolbarModule,
	MatTooltipModule
} from "@angular/material";
import { NguUtilityModule } from "ngu-utility/ngu-utility.module";
import { MalihuScrollbarModule } from "ngx-malihu-scrollbar";

// angular2-moment
// https://github.com/urish/angular2-moment
// import { MomentModule } from "angular2-moment";


//Form Validation
// https://github.com/yuyang041060120/ng2-validation

//ng-daterangepicker
//https://github.com/jkuri/ng-daterangepicker


// UI Shared Components
import { FooterComponent } from "../layout/footer/footer.component";
import { AppBackdropComponent } from "./components/app_backdrop/app_backdrop.component";
import { Profile } from "./components/profile/profile.component";
import { ImageCardComponent } from "./components/cards/image-card/image-card.component";
import { TabsOverCardComponent } from "./components/cards/tabs-over-card/tabs-over-card.component";
import { SocialCardComponent } from "./components/cards/social-card/social-card.component";
import { ConfirmDialogComponent } from "./components/confirm/confirm.component";
import {
	SmdFabSpeedDialActionsComponent,
	SmdFabSpeedDialComponent,
	SmdFabSpeedDialTriggerComponent
} from "./components/fab/index";

@NgModule({
	imports: [
		CommonModule,
		FormsModule,
		ReactiveFormsModule,
		MatAutocompleteModule,
		MatButtonModule,
		MatButtonToggleModule,
		MatCardModule,
		MatCheckboxModule,
		MatChipsModule,
		MatDatepickerModule,
		MatDialogModule,
		MatExpansionModule,
		MatGridListModule,
		MatIconModule,
		MatInputModule,
		MatListModule,
		MatMenuModule,
		MatNativeDateModule,
		MatProgressBarModule,
		MatProgressSpinnerModule,
		MatRadioModule,
		MatRippleModule,
		MatSelectModule,
		MatSidenavModule,
		MatSliderModule,
		MatSlideToggleModule,
		MatSnackBarModule,
		MatTabsModule,
		MatToolbarModule,
		MatTooltipModule,
		NguUtilityModule,
		NgbModule.forRoot(),
		MalihuScrollbarModule.forRoot(),
		FlexLayoutModule
	],
	declarations: [
		AppBackdropComponent,
		Profile,
		FooterComponent,
		ImageCardComponent,
		TabsOverCardComponent,
		SocialCardComponent,
		SmdFabSpeedDialActionsComponent,
		SmdFabSpeedDialComponent,
		SmdFabSpeedDialTriggerComponent,
		ConfirmDialogComponent
	],
	exports: [
		CommonModule,
		FormsModule,
		MatAutocompleteModule,
		MatButtonModule,
		MatButtonToggleModule,
		MatCardModule,
		MatCheckboxModule,
		MatChipsModule,
		MatDatepickerModule,
		MatDialogModule,
		MatExpansionModule,
		MatGridListModule,
		MatIconModule,
		MatInputModule,
		MatListModule,
		MatMenuModule,
		MatNativeDateModule,
		MatProgressBarModule,
		MatProgressSpinnerModule,
		MatRadioModule,
		MatRippleModule,
		MatSelectModule,
		MatSidenavModule,
		MatSliderModule,
		MatSlideToggleModule,
		MatSnackBarModule,
		MatTabsModule,
		MatToolbarModule,
		MatTooltipModule,
		NguUtilityModule,
		AppBackdropComponent,
		Profile,
		FooterComponent,
		ImageCardComponent,
		TabsOverCardComponent,
		SocialCardComponent,
		ReactiveFormsModule,
		MalihuScrollbarModule,
		NgbModule,
		FlexLayoutModule,
		SmdFabSpeedDialActionsComponent,
		SmdFabSpeedDialComponent,
		SmdFabSpeedDialTriggerComponent
	],
    entryComponents: [
        ConfirmDialogComponent
    ]
})
export class SharedModule {
	static forRoot(): ModuleWithProviders {
		return {
			ngModule: SharedModule
		};
	}
}
