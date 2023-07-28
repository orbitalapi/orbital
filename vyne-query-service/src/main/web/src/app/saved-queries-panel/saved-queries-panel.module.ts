import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SavedQueriesPanelComponent } from './saved-queries-panel.component';



@NgModule({
    declarations: [
        SavedQueriesPanelComponent
    ],
    exports: [
        SavedQueriesPanelComponent
    ],
    imports: [
        CommonModule
    ]
})
export class SavedQueriesPanelModule { }
