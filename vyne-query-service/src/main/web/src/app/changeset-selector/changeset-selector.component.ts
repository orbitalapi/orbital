import { Component, OnInit } from '@angular/core';
import { Changeset, ChangesetService } from 'src/app/changeset-selector/changeset.service';

@Component({
  selector: 'app-changeset-selector',
  templateUrl: './changeset-selector.component.html',
  styleUrls: ['./changeset-selector.component.scss']
})
export class ChangesetSelectorComponent implements OnInit {

  availableChangesets$ = this.changesetService.availableChangesets$;

  value: Changeset | null = null;

  constructor(private changesetService: ChangesetService) {
  }

  ngOnInit(): void {
    // TODO Don't subscribe here but rather connect the two observables
    this.changesetService.activeChangeset$
      .subscribe(value => {
        this.value = value;
      });
  }

  selectChangeset(changeset: Changeset): void {
    this.changesetService.setActiveChangeset(changeset)
      .subscribe();

  }
}
