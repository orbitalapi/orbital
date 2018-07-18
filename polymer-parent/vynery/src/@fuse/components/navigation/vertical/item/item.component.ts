import { Component, HostBinding, Input } from '@angular/core';

import { FuseNavigationItem } from '@fuse/types';

@Component({
    selector   : 'fuse-nav-vertical-item',
    templateUrl: './item.component.html',
    styleUrls  : ['./item.component.scss']
})
export class FuseNavVerticalItemComponent
{
    @HostBinding('class')
    classes = 'nav-item';

    @Input()
    item: FuseNavigationItem;

    /**
     * Constructor
     */
    constructor()
    {
    }
}
