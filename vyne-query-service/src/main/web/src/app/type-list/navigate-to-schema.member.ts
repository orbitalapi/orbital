import {SchemaMemberReference} from "../services/schema";
import {Router} from "@angular/router";

export function navigateToSchemaMember(member: SchemaMemberReference, router: Router) {
  router.navigate(getRouterLink(member))

}


export function getRouterLink(member: SchemaMemberReference): string[] {
  switch (member.kind) {
    case 'SERVICE':
      return ['/services', member.qualifiedName.fullyQualifiedName];
    case 'OPERATION':
      const parts = member.qualifiedName.fullyQualifiedName
        .replace('@@', '/')
        .split('/');
      const serviceName = parts[0].trim();
      const operationName = parts[1].trim();
      return ['/services', serviceName, operationName];
    default:
      return ['/catalog', member.qualifiedName.fullyQualifiedName];
  }
}
