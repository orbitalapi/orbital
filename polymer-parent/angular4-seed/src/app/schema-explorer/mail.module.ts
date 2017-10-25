import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { RouterModule } from "@angular/router";
import { MailComponent } from "./mail.component";
import { SharedModule } from "../../shared/shared.module";
import { Ng2SearchPipeModule } from "ng2-search-filter";

const MAIL_ROUTE = [{ path: "", component: MailComponent }];

@NgModule({
	declarations: [MailComponent],
	imports: [
		CommonModule,
		SharedModule,
		Ng2SearchPipeModule,
		RouterModule.forChild(MAIL_ROUTE)
	]
})
export class MailModule {}
