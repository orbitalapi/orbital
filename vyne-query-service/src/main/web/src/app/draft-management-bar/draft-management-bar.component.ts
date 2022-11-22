import { Component, OnInit } from '@angular/core';
import { TypesService } from '../services/types.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ChangesetService } from 'src/app/services/changeset.service';

@Component({
  selector: 'app-draft-management-bar',
  templateUrl: './draft-management-bar.component.html',
  styleUrls: ['./draft-management-bar.component.scss'],
})
export class DraftManagementBarComponent implements OnInit {

  constructor(public changesetService: ChangesetService, private snackBar: MatSnackBar) {
  }

  ngOnInit(): void {
  }

  commitChanges() {
    this.changesetService.finalizeChangeset().subscribe((response) => {
      this.snackBar.open('Changes pushed', 'Dismiss', { duration: 10000 });
      if (response.link !== null) {
        this.openPr(response.link);
      }
    });
  }

  backToMain() {
    this.changesetService.selectDefaultChangeset();
  }

  private openPr(link: string): void {
    const didTabOpenSuccessfully = window.open(link, '_blank');
    if (didTabOpenSuccessfully !== null) {
      didTabOpenSuccessfully.focus();
    } else {
      this.snackBar.open(
        'Failed to open the PR. You can find it on this link: ' + link,
        'Dismiss',
        { duration: 30000 },
      );
    }
  }
}
