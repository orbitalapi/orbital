import {Directive, Input, OnInit, TemplateRef, ViewContainerRef} from '@angular/core';
import {Privilege, UserInfoService, VynePrivileges} from "./services/user-info.service";
import {AuthService} from "./auth/auth.service";

@Directive({
  selector: '[appRequiresAuthority]',
  standalone: true
})
export class RequiresAuthorityDirective implements OnInit {

  @Input('appRequiresAuthority')
  requiredAuthorities: Privilege[] = [];

  constructor(
    private readonly userInfoService: UserInfoService,
    private readonly templateRef: TemplateRef<any>,
    private readonly viewContainer: ViewContainerRef,
  ) {

  }

  ngOnInit(): void {
    this.userInfoService.userInfo$.subscribe({
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
    this.viewContainer.createEmbeddedView(this.templateRef);
  }

  private hideComponent() {
    this.viewContainer.clear();
  }
}
