import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { concatMap, Observable } from 'rxjs';
import { shareReplay, tap } from 'rxjs/operators';
import { TuiAccordionModule } from '@taiga-ui/kit';
import { UiCustomisations } from '../../../../environments/ui-customisations';
import { PackagesService, SourcePackageDescription } from '../../../package-viewer/packages.service';
import { TypesService } from '../../../services/types.service';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [CommonModule, TuiAccordionModule],
  templateUrl: './project-list.component.html',
  styleUrls: ['./project-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProjectListComponent {
  packages$: Observable<SourcePackageDescription[]>
  @Input()
  isListOpen: boolean;
  @Output()
  projectCountUpdated: EventEmitter<number> = new EventEmitter<number>();

  constructor(private packagesService: PackagesService, private typesService: TypesService) {
    // NOTE: the getTypes observable emits when the Project has been added successfully,
    //       and is required to get around the async nature of POSTing a new project
    this.packages$ = this.typesService.getTypes()
      .pipe(
        concatMap(val => this.packagesService.listPackages()),
        tap(value => this.projectCountUpdated.emit(value.length)),
        shareReplay()
      )
  }

  getSourceDescription(sourcePackage:SourcePackageDescription):string {
    switch (sourcePackage.publisherType) {
      case 'FileSystem': return 'Read from disk'
      case 'GitRepo':
        return 'Git repo';
      case 'Pushed':
        return `Pushed to ${UiCustomisations.productName}`;
    }
  }
  getSourceIcon(sourcePackage: SourcePackageDescription) {
    switch (sourcePackage.publisherType) {
      case 'FileSystem': return 'assets/img/tabler/files.svg'
      case 'GitRepo': return 'assets/img/tabler/git-merge.svg'
      case 'Pushed': return 'assets/img/tabler/rss.svg'
    }
  }
}
