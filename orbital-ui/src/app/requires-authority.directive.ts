import {DestroyRef, Directive, Input, OnInit, TemplateRef, ViewContainerRef} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Privilege, UserInfoService, VynePrivileges} from "./services/user-info.service";

@Directive({
  selector: '[appRequiresAuthority]',
  standalone: true
})
export class RequiresAuthorityDirective implements OnInit {

  @Input('appRequiresAuthority')
  requiredAuthorities: Privilege[] = [];

  private isShown: boolean

  constructor(
    private readonly userInfoService: UserInfoService,
    private readonly templateRef: TemplateRef<any>,
    private readonly viewContainer: ViewContainerRef,
    private readonly destroyRef: DestroyRef,
  ) {

  }

  ngOnInit(): void {
    this.userInfoService.userInfo$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: user => {
          const permitted = this.requiredAuthorities.some(requiredAuth => user.grantedAuthorities.includes(requiredAuth as VynePrivileges))
          if (permitted) {
            this.showComponent()
          } else {
            this.hideComponent()
          }
        }
      })
  }

  private showComponent() {
    if (this.isShown) return;
    this.isShown = true;
    this.viewContainer.createEmbeddedView(this.templateRef);
  }

  private hideComponent() {
    this.isShown = false;
    this.viewContainer.clear();
  }
}
