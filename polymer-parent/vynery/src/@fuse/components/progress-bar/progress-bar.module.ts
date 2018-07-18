import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { MatButtonModule, MatIconModule, MatProgressBarModule } from '@angular/material';

import { FuseProgressBarComponent } from './progress-bar.component';

@NgModule({
    declarations: [
        FuseProgressBarComponent
    ],
    imports     : [
        CommonModule,
        RouterModule,

        MatButtonModule,
        MatIconModule,
        MatProgressBarModule
    ],
    exports     : [
        FuseProgressBarComponent
    ]
})
export class FuseProgressBarModule
{
}
