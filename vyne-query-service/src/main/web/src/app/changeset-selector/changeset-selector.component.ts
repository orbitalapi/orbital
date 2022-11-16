import { Component, OnInit } from '@angular/core';
import { TuiIdentityMatcher } from '@taiga-ui/cdk';
import { Changeset, TypesService } from '../services/types.service';

@Component({
  selector: 'app-changeset-selector',
  templateUrl: './changeset-selector.component.html',
  styleUrls: ['./changeset-selector.component.scss']
})
export class ChangesetSelectorComponent implements OnInit {

  availableChangesets$ = this.typesService.availableChangesets$;

  value: Changeset | null = null;

  constructor(private typesService: TypesService) { }

  ngOnInit(): void {
    this.typesService.activeChangeset$
      .subscribe(value => {
        this.value = value;
      });
  }

  selectChangeset(changeset: Changeset): void {
    this.typesService.setActiveChangeset(changeset)
      .subscribe(() =>
        location.reload() // TODO This is only needed due to the fact that schema updates are not reflected on the page
      );

  }
}
