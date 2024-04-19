import { Component, HostBinding, Input } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-card',
  standalone: true,
  imports: [CommonModule, RouterLink, NgOptimizedImage],
  templateUrl: './card.component.html',
  styleUrls: ['./card.component.scss']
})
export class CardComponent {

  @Input()
  title: string;
  // Prevents tooltip displaying in browser
  @HostBinding('attr.title') get getTitle(): null {
    return null;
  }

  @Input()
  titleLink: string;

  @Input()
  description: string;

  @Input()
  icon: string;
}
