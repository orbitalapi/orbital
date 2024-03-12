import { Component, DestroyRef, HostBinding, Input } from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {CompilationMessage, ParsedSource, VersionedSource} from '../services/schema';
import {FilenameWithDecorators} from "./file-tree.component";

declare const require: any;
/* eslint-disable-next-line */
// let hljs: any = require('highlight.js/lib');
// hljs.registerLanguage('taxi', taxiLangDef);

export type CodeViewerFlexBoxMode = 'grid' | 'flex';

@Component({
  selector: 'app-code-viewer',
  templateUrl: './code-viewer.component.html',
  styleUrls: ['./code-viewer.component.scss']
})
export class CodeViewerComponent {

  private _sources: ParsedSource[] | VersionedSource[];

  parsedSources: ParsedSource[];
  filenames: FilenameWithDecorators[];

  errors: CompilationMessage[]

  @Input()
  get sources(): ParsedSource[] | VersionedSource[] {
    return this._sources;
  }

  set sources(value: ParsedSource[] | VersionedSource[]) {
    this._sources = value;
    this.parsedSources = convertToParsedSources(this.sources);
    this.filenames = this.parsedSources.map(s => {
      return {
        filename: s.source.name,
        errorCount: s.errors.length
      }
    });
    this.errors = this.parsedSources.flatMap(s => s.errors);

    if (this.parsedSources && this.parsedSources.length > 0 && !this.selectedFilename) {
      this.select(this.parsedSources[0].source.name);
    } else if (this.selectedFilename) {
      this.activateSelectedSource();
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
              private destroyRef: DestroyRef
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
        return this._sources && this._sources.length > 1;
      case 'Visible':
        return true;
      case 'Hidden' :
        return false;
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
}

export type SidebarMode = 'Visible' | 'Hidden' | 'Auto';


function isVersionedSource(source: ParsedSource | VersionedSource): source is VersionedSource {
  if (!source) {
    return false;
  }
  return !isParsedSource(source);
}

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

function versionedSource(input: ParsedSource | VersionedSource): VersionedSource {
  if (isVersionedSource(input)) {
    return input;
  } else {
    return (input as ParsedSource).source;
  }
}

function convertToParsedSources(input: ParsedSource[] | VersionedSource[]): ParsedSource[] {
  return input.map((s: ParsedSource | VersionedSource) => parsedSource(s))
}
