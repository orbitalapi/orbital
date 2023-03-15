import { Pipe, PipeTransform } from '@angular/core';
import { DiffKind } from './changelog.service';
import { TitleCasePipe } from '@angular/common';

@Pipe({
  name: 'diffKind'
})
export class DiffKindPipe implements PipeTransform {

  private titleCasePipe = new TitleCasePipe();
  transform(value: DiffKind): string {
    const split = value
      // insert a space before all caps
      .replace(/([A-Z])/g, ' $1')
    return this.titleCasePipe.transform(split);
  }

}
