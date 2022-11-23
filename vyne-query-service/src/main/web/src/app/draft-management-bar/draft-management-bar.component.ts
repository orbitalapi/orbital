import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ChangesetService } from 'src/app/services/changeset.service';
import * as moment from 'moment';

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
      this.openPullRequest(response.link);
    });
  }

  backToMain() {
    this.changesetService.selectDefaultChangeset();
  }

  getCreatedAtString() {
    const lastUpdatedString = this.changesetService.activeBranchOverview?.lastUpdated;
    if (!lastUpdatedString) {
      return '';
    }
    const lastUpdated = moment(Date.parse(lastUpdatedString));
    const now = moment(new Date());
    if (moment.duration(now.diff(lastUpdated)).asMinutes() < 15) {
      return 'Just now';
    } else {
      return lastUpdated.format('DD/MM/YYYY HH:mm');
    }
  }

  private openPullRequest(link: string): void {
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
