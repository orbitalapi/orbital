import {CommonModule} from '@angular/common';
import {Component} from '@angular/core';
import {TuiHintDirective, TuiNotification} from '@taiga-ui/core';
import {ClusterMembersService} from './cluster-members.service';

@Component({
  selector: 'app-cluster-members-display',
  standalone: true,
  imports: [
    CommonModule,
    TuiHintDirective,
    TuiNotification,
  ],
  templateUrl: './cluster-members-display.component.html',
  styleUrl: './cluster-members-display.component.scss'
})
export class ClusterMembersDisplayComponent {
  constructor(
    public clusterMembersService: ClusterMembersService
  ) {
  }

  clustersPluralMap: {[k: string]: string} = {
    '=1' : 'is 1 node',
    'other' : 'are # nodes'
  }

}
