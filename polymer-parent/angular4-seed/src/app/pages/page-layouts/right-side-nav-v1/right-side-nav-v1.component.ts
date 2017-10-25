import {
	Component,
	ViewEncapsulation,
	ViewChild,
	OnInit,
	HostListener,
	ElementRef,
	Renderer,
	Inject
} from "@angular/core";
import { GlobalState } from "../../../app.state";
import { ConfigService } from "../../../shared/services/config/config.service";
import { MdSidenav } from "@angular/material";

@Component({
	selector: ".content_inner_wrapper",
	templateUrl: "./right-side-nav-v1.component.html",
	styleUrls: ["./right-side-nav-v1.component.scss"],
	encapsulation: ViewEncapsulation.Emulated
})
export class RightSideNavV1Component implements OnInit {
	@ViewChild("t") t;
	@ViewChild("rightSidenav1") rightSidenav1: MdSidenav;
	@ViewChild("menuTabs") private allMElementRef;
	navMode = "side";
	isActiveTab = false;
	vertical: any;
	tabLabels = [
		{ activeId: "tab_0",
			label: "Sidenav item 1" },
		{ activeId: "tab_1",
			label: "Sidenav item 2" },
		{ activeId: "tab_2",
			label: "Sidenav item 3" }
	];
	selectTab(activeId, event) {
		this.t.select(activeId);
	}
	
	constructor(
		@Inject(Renderer) private renderer: Renderer,
		public config: ConfigService,
		private _elementRef: ElementRef,
		private _state: GlobalState
	) { }
	ngAfterViewInit() {
		window.setTimeout(() =>
			this.renderer.invokeElementMethod(
				this.allMElementRef.nativeElement,
				"click",
				[]
			)
		);
	}
	ngOnInit() {
		if (window.innerWidth < 992) {
			this.navMode = "over";
			this.rightSidenav1.opened = false;
		}
		if (window.innerWidth > 992) {
			this.navMode = "side";
			this.rightSidenav1.open();
		}
	}

	@HostListener("window:resize", ["$event"])
	onResize(event) {
		if (event.target.innerWidth < 992) {
			this.navMode = "over";
			this.rightSidenav1.close();
		}
		if (event.target.innerWidth > 992) {
			this.navMode = "side";
			this.rightSidenav1.open();
		}
	}
}
