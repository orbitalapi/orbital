import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {ComponentLifecycleEvent, ComponentState} from "../../services/stubs-api.service";
import {NgClass} from "@angular/common";
import {SvgIconComponent} from "../../svg-icon/svg-icon.component";

@Component({
  selector: 'app-component-state-icon',
  standalone: true,
  imports: [
    NgClass,
    SvgIconComponent
  ],
  template: `
    <app-svg-icon
      [src]="stateIcon.img"
      [ngClass]="stateIcon.className"
      [width]="16"
      [height]="16"
      [strokeWidth]="2"
      [title]="state.state"
    ></app-svg-icon>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './component-state-icon.component.scss'
})
export class ComponentStateIconComponent {

  @Input()
  state: ComponentLifecycleEvent

  get stateIcon():StateIcon {
    if (this.state == null) {
      return this.stateIcons["NotStarted"]
    }
    return this.stateIcons[this.state?.state]
  }
  private stateIcons:{[index in ComponentState]: StateIcon} = {
    NotStarted: {
      className: 'not-started',
      label: 'Not started',
      img: 'assets/img/tabler/player-stop.svg'
    },
    Starting: {
      className: 'starting in-progress',
      label: 'Starting',
      img: 'assets/img/tabler/player-play.svg'
    },
    Running: {
      className: 'running',
      label: 'Running',
      img: 'assets/img/tabler/player-play.svg'
    },
    Stopping: {
      className: 'stopping in-progress',
      label: 'Stopping',
      img: 'assets/img/tabler/player-stop.svg'
    },
    Stopped: {
      className: 'stopped',
      label: 'Stopped',
      img: 'assets/img/tabler/player-stop.svg'
    },
    Failed: {
      className: 'failed',
      label: 'Failed',
      img: 'assets/img/tabler/exclamation-circle.svg'
    }
  }

}

interface StateIcon {
  label: string;
  className: string;
  img: string;
}
