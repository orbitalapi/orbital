import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ModelDesignerComponent} from './model-designer.component';
import {RouterModule} from "@angular/router";
import {HeaderBarModule} from "../header-bar/header-bar.module";
import {SourceInputPanelComponent} from './source-input-panel/source-input-panel.component';
import {CodeEditorPanelComponent} from './code-editor-panel/code-editor-panel.component';
import {ParseResultPanelComponent} from './parse-result-panel/parse-result-panel.component';
import {AngularSplitModule} from "angular-split";
import {
    TuiComboBoxModule,
    TuiDataListWrapperModule,
    TuiFilterByInputPipeModule,
    TuiInputFilesModule, TuiProgressModule, TuiStringifyContentPipeModule
} from "@taiga-ui/kit";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {
    TuiButtonModule,
    TuiDataListModule,
    TuiLoaderModule,
    TuiNotificationModule,
    TuiTextfieldControllerModule
} from "@taiga-ui/core";
import {ExpandingPanelSetModule} from "../expanding-panelset/expanding-panel-set.module";
import {CodeEditorModule} from "../code-editor/code-editor.module";
import {TaxiParserService} from "./taxi-parser.service";
import {CompilationMessageListModule} from "../compilation-message-list/compilation-message-list.module";
import {TabbedResultsViewModule} from "../tabbed-results-view/tabbed-results-view.module";
import {TypeNamedInstanceTreeModule} from "../type-named-instance-tree/type-named-instance-tree.module";
import {JsonViewerModule} from "../json-viewer/json-viewer.module";

@NgModule({
    declarations: [
        ModelDesignerComponent,
        SourceInputPanelComponent,
        CodeEditorPanelComponent,
        ParseResultPanelComponent,
    ],
    providers: [
        TaxiParserService
    ],
    imports: [
        CommonModule,
        RouterModule.forChild([
            {
                path: '',
                component: ModelDesignerComponent,
            },
        ]),
        HeaderBarModule,
        AngularSplitModule,
        TuiInputFilesModule,
        ReactiveFormsModule,
        TuiButtonModule,
        ExpandingPanelSetModule,
        CodeEditorModule,
        TuiComboBoxModule,
        FormsModule,
        TuiDataListWrapperModule,
        TuiFilterByInputPipeModule,
        TuiStringifyContentPipeModule,
        TuiTextfieldControllerModule,
        TuiNotificationModule,
        TuiProgressModule,
        CompilationMessageListModule,
        TabbedResultsViewModule,
        TuiLoaderModule,
        TypeNamedInstanceTreeModule,
        JsonViewerModule,
        TuiDataListModule
    ]
})
export class ModelDesignerModule {
}
