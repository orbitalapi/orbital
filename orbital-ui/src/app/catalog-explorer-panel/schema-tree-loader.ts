import {Injectable} from "@angular/core";
import {TuiTreeLoader} from "@taiga-ui/kit";
import {SchemaTreeNode, TypesService} from "../services/types.service";
import {Observable} from "rxjs";

@Injectable()
export class SchemaTreeLoader implements TuiTreeLoader<SchemaTreeNode> {
  constructor(private service: TypesService) {
  }

  hasChildren(item: SchemaTreeNode): boolean {
    return item.hasChildren;
  }

  loadChildren(item: SchemaTreeNode): Observable<SchemaTreeNode[]> {
    // Root node
    if (item['rootNode']) {
      return this.service.getSchemaTree(null);
    } else {
      return this.service.getSchemaTree(item.element.parameterizedName);
    }

  }

}
