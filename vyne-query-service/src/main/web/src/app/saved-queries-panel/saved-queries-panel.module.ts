import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SavedQueriesPanelComponent } from './saved-queries-panel.component';
import { SavedQueryComponent } from './saved-query.component';
import {TruncatePipeModule} from "../truncate-pipe/truncate-pipe.module";



@NgModule({
    declarations: [
        SavedQueriesPanelComponent,
        SavedQueryComponent
    ],
    exports: [
        SavedQueriesPanelComponent
    ],
    imports: [
        CommonModule,
        TruncatePipeModule
    ]
})
export class SavedQueriesPanelModule { }
