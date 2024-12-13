import { TuiTextfieldControllerModule, TuiComboBoxModule } from "@taiga-ui/legacy";
import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ModelDesignerComponent} from './model-designer.component';
import {RouterModule} from "@angular/router";
import {HeaderBarModule} from "../header-bar/header-bar.module";
import {SourceInputPanelComponent} from './source-input-panel/source-input-panel.component';
import {CodeEditorPanelComponent} from './code-editor-panel/code-editor-panel.component';
import {ParseResultPanelComponent} from './parse-result-panel/parse-result-panel.component';
import {AngularSplitModule} from "angular-split";
import { TuiDataListWrapper, TuiStringifyContentPipe, TuiFilterByInputPipe, TuiProgress, TuiFiles } from "@taiga-ui/kit";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import { TuiNotification, TuiDataList, TuiLoader, TuiLink, TuiButton, TuiHint } from '@taiga-ui/core';
import {ExpandingPanelSetModule} from "../expanding-panelset/expanding-panel-set.module";
import {CodeEditorModule} from "../code-editor/code-editor.module";
import {TaxiParserService} from "./taxi-parser.service";
import {CompilationMessageListModule} from "../compilation-message-list/compilation-message-list.module";
import {TabbedResultsViewModule} from "../tabbed-results-view/tabbed-results-view.module";
import {TypeNamedInstanceTreeModule} from "../type-named-instance-tree/type-named-instance-tree.module";
import {JsonViewerModule} from "../json-viewer/json-viewer.module";
import {TypeAutocompleteTuiComponent} from '../type-autocomplete-tui/type-autocomplete-tui.component';
import { UiCustomisations } from '../../environments/ui-customisations';

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
                title: `${UiCustomisations.productName}: Designer`
            },
        ]),
        HeaderBarModule,
        AngularSplitModule,
        ...TuiFiles,
        ReactiveFormsModule,
        TuiButton,
        ExpandingPanelSetModule,
        CodeEditorModule,
        TuiComboBoxModule,
        FormsModule,
        ...TuiDataListWrapper,
        TuiFilterByInputPipe,
        TuiStringifyContentPipe,
        TuiTextfieldControllerModule,
        TuiNotification,
        ...TuiProgress,
        CompilationMessageListModule,
        TabbedResultsViewModule,
        TuiLoader,
        TypeNamedInstanceTreeModule,
        JsonViewerModule,
        ...TuiDataList,
        TypeAutocompleteTuiComponent,
        ...TuiHint,
        TuiLink
    ]
})
export class ModelDesignerModule {
}
