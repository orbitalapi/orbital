import { Component, HostBinding, Input } from '@angular/core';

import { FuseNavigationItem } from '@fuse/types';

@Component({
    selector   : 'fuse-nav-vertical-group',
    templateUrl: './group.component.html',
    styleUrls  : ['./group.component.scss']
})
export class FuseNavVerticalGroupComponent
{
    @HostBinding('class')
    classes = 'nav-group nav-item';

    @Input()
    item: FuseNavigationItem;

    /**
     * Constructor
     */
    constructor()
    {
    }

}
