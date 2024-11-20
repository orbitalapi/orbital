import {ChangeDetectionStrategy, Component, computed, input, Signal} from '@angular/core';
import {findSchemaMember, QualifiedName, Schema} from "../services/schema";
import {TypesService} from "../services/types.service";
import {toSignal} from "@angular/core/rxjs-interop";
import {QualifiedNameParser} from "../services/qualified-name-parser";
import {CommonModule, TitleCasePipe} from '@angular/common';
import {memberType, memberTypeForCSS} from "../type-list/type-list.component";
import {getTypeNameToView} from "../type-viewer/type-viewer.component";
import {RouterLink} from "@angular/router";

@Component({
  selector: 'app-schema-member-name-badge',
  standalone: true,
  imports: [
    CommonModule,
    TitleCasePipe,
    RouterLink,
  ],
  host: {'class': 'mono-badge'},
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ng-container *ngIf="schema()">
      <a (click)="$event.stopImmediatePropagation();" [routerLink]="route()">{{ displayName() }}</a>
      <div class="badge" [ngClass]="memberTypeForCSS(schemaMember())">
        {{ memberType() | titlecase }}
      </div>
    </ng-container>
  `,
  styleUrl: './schema-member-name-badge.component.scss'
})
export class SchemaMemberNameBadgeComponent {
  protected readonly schema: Signal<Schema>;

  constructor(typeService: TypesService) {
    this.schema = toSignal(typeService.getSchema())
  }
  member = input.required<QualifiedName | string>();
  showFullyQualifiedName = input(true)
  route = computed(() => {
    const qualifiedName = this.qualifiedName();
    const typeNameToView = getTypeNameToView(qualifiedName);
    return ['/catalog', typeNameToView.fullyQualifiedName]
  })

  qualifiedName = computed(() => {
    const member = this.member();
    if (typeof(member) === 'string') {
      return QualifiedNameParser.parse(member);
    } else {
      return member
    }
  })

  memberType = computed(() => {
    return memberType(this.schemaMember())
  })

  schemaMember = computed(() => {
    return findSchemaMember(this.schema(), this.qualifiedName().parameterizedName)
  })

  displayName = computed(() => {
    const qualifiedName = this.qualifiedName();
    return (this.showFullyQualifiedName()) ? qualifiedName.longDisplayName : qualifiedName.shortDisplayName;
  })
  protected readonly memberTypeForCSS = memberTypeForCSS;
}
