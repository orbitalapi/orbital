import {CommonModule} from '@angular/common';
import {ChangeDetectionStrategy, ChangeDetectorRef, Component} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Router, RouterLink, RouterOutlet} from '@angular/router';
import {TuiButtonModule, TuiNotificationModule} from '@taiga-ui/core';
import {UiCustomisations} from '../../environments/ui-customisations';
import {HeaderComponentLayoutModule} from '../header-component-layout/header-component-layout.module';
import {OnboardingContainerComponent} from '../onboarding/onboarding-container.component';
import {TypesService} from '../services/types.service';
import {ChangelogCardComponent} from './changelog-card/changelog-card.component';
import {DataSourcesCardComponent} from './data-sources-card/data-sources-card.component';
import {EndpointStatsCardComponent} from './endpoint-stats-card/endpoint-stats-card.component';
import {CardComponent} from "./card/card.component";
import {ContentCardComponent} from "./content-card/content-card.component";
import {ContentCardData, ContentService} from "../services/content.service";
import {Observable} from "rxjs";
import {RequiresAuthorityDirective} from "../requires-authority.directive";

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, HeaderComponentLayoutModule, RouterOutlet, TuiButtonModule, RouterLink,
    TuiNotificationModule, DataSourcesCardComponent, EndpointStatsCardComponent, ChangelogCardComponent, CardComponent, ContentCardComponent,
    RequiresAuthorityDirective
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent {
  hideLiveReloadNotification: boolean;

  readonly uiConfig = UiCustomisations;
  private readonly HIDE_LIVE_RELOAD_NOTIFICATION_LOCAL_STORAGE_KEY = "hideLiveReloadNotification";

  contentCards$: Observable<ContentCardData[]>

  constructor(
    private typeService: TypesService,
    private router: Router,
    private changeDetectorRef: ChangeDetectorRef,
    private contentService: ContentService
  ) {
    this.contentCards$ = contentService.getContent();
    typeService.getTypes()
      .pipe(takeUntilDestroyed())
      .subscribe(type => {
        const hideOnboarding = localStorage.getItem(OnboardingContainerComponent.IS_ONBOARDING_HIDDEN_LOCAL_STORAGE_KEY) === "true";
        if (type.services.length === 0 && !hideOnboarding) {
          router.navigate(
            ['/onboarding'],
            {
              replaceUrl: true,
              queryParams: {
                isOnboarding: true
              }
            }
          );
        }
      });

    this.hideLiveReloadNotification = localStorage.getItem(this.HIDE_LIVE_RELOAD_NOTIFICATION_LOCAL_STORAGE_KEY) === "true";
  }

  onHideNotification() {
    localStorage.setItem(this.HIDE_LIVE_RELOAD_NOTIFICATION_LOCAL_STORAGE_KEY, "true");
    this.hideLiveReloadNotification = true;
  }
}
