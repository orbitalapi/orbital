import {ChangeDetectionStrategy, ChangeDetectorRef, Component, DestroyRef, HostBinding, Input} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {CompilationMessage, ParsedSource, VersionedSource} from '../services/schema';
import {FileTreeNode, isFileTreeNodeList, sourcesToFileTreeNode} from "./file-tree.component";
import {isNullOrUndefined} from "util";

declare const require: any;
/* eslint-disable-next-line */
// let hljs: any = require('highlight.js/lib');
// hljs.registerLanguage('taxi', taxiLangDef);

export type CodeViewerFlexBoxMode = 'grid' | 'flex';

@Component({
  selector: 'app-code-viewer',
  templateUrl: './code-viewer.component.html',
  styleUrls: ['./code-viewer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CodeViewerComponent {

  private _sources: ParsedSource[] | VersionedSource[] | FileTreeNode[]

  // Do not set directly - call setParsedSources()
  parsedSources: ParsedSource[];

  errors: CompilationMessage[]

  get fileTreeNodes():FileTreeNode[] {
    if (isFileTreeNodeList(this.sources)) {
      return this.sources;
    } else {
      const rootNode = sourcesToFileTreeNode(this.parsedSources, '/');
      return [rootNode];
    }

  }

  @Input()
  get sources(): ParsedSource[] | VersionedSource[] | FileTreeNode[] {
    return this._sources;
  }

  set sources(value) {
    this._sources = value;

    if (isFileTreeNodeList(this.sources)) {
      this.setFromFileTreeNodeList(this.sources);
    } else {
      this.setParsedSources(convertToParsedSources(this.sources))
    }

  }

  @Input()
  sidebarMode: SidebarMode = 'Auto';

  @Input()
  flexboxMode: CodeViewerFlexBoxMode = 'flex';

  @Input()
  useRouter: boolean = false;

  @HostBinding('class.flex-grid') get className() {
    return this.flexboxMode === 'grid'
  }

  selectedSource: ParsedSource;
  selectedFilename: string;

  constructor(private activatedRoute: ActivatedRoute,
              private router: Router,
              private destroyRef: DestroyRef,
              private changeDetector: ChangeDetectorRef
  ) {
    activatedRoute.queryParams
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        this.selectedFilename = params['selectedFile']
        if (this.selectedFilename) {
          this.activateSelectedSource()
        }
      });
  }


  get displaySidebar(): boolean {
    switch (this.sidebarMode) {
      case 'Auto':
        return this._sources && this.sourcesCount > 1;
      case 'Visible':
        return true;
      case 'Hidden' :
        return false;
    }
  }

  get sourcesCount():number {
    if (isNullOrUndefined(this.parsedSources)) {
      return 0;
    } else {
      return this.parsedSources.length
    }
  }

  get sourceContent(): string {
    if (!this.selectedSource) {
      return '';
    } else {
      return this.selectedSource.source.content;
    }
  }

  private activateSelectedSource() {
    if (!this.parsedSources || this.parsedSources.length == 0)
      return;
    if (!this.selectedFilename)
      return;

    this.selectedSource = this.parsedSources.find(s => s.source.name === this.selectedFilename)
  }

  select(filename: string) {
    if (this.useRouter) {
      this.router.navigate([],
        {
          relativeTo: this.activatedRoute,
          queryParams: {
            'selectedFile': filename
          },
          queryParamsHandling: "merge",
          replaceUrl: true
        },
      );
    } else {
      this.selectedFilename = filename;
      this.activateSelectedSource();
    }
  }

  compilationMessageClicked($event: CompilationMessage) {
    if ($event.sourceName.startsWith('[')) {
      // name in the format of:
      // [demo.vyne/films-demo/0.1.0]/src/film/FilmService.taxi
      const filename = ($event.sourceName.split(']')[1]).substring(1) // remove the preceding /
      this.select(filename)
    } else {
      this.select($event.sourceName.substring(1)); // remove the preceding /
    }

  }

  private setFromFileTreeNodeList(fileTreeNodes: FileTreeNode[]) {
    // Build the list of parsed sources from the file tree
    const parsedSources = fileTreeNodes.flatMap(fileTreeNode => {
      return [fileTreeNode].concat(...fileTreeNode.descendants);
    })
      .map(fileTreeNode => {
        return fileTreeNode.value?.source;
      })
      .filter(parsedSource => !isNullOrUndefined(parsedSource))
    this.setParsedSources(parsedSources);
  }

  private setParsedSources(parsedSources: ParsedSource[]) {
    this.parsedSources = parsedSources;
    this.errors = this.parsedSources.flatMap(s => s.errors);

    if (this.parsedSources && this.parsedSources.length > 0 && !this.selectedFilename) {
      this.select(this.parsedSources[0].source.name);
    } else if (this.selectedFilename) {
      this.activateSelectedSource();
    }
  }
}

export type SidebarMode = 'Visible' | 'Hidden' | 'Auto';

function isParsedSource(source: ParsedSource | VersionedSource): source is ParsedSource {
  if (!source) {
    return false;
  }
  return (source as ParsedSource).source !== undefined && (source as ParsedSource).errors !== undefined;
}

function parsedSource(input: ParsedSource | VersionedSource): ParsedSource {
  if (isParsedSource(input))
    return input;
  return {
    source: input,
    errors: [],
    isValid: true
  }
}

export function convertToParsedSources(input: ParsedSource[] | VersionedSource[]): ParsedSource[] {
  return input.map((s: ParsedSource | VersionedSource) => parsedSource(s))
}
