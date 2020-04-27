import {AfterViewInit, ElementRef, EventEmitter, OnDestroy, Output, ViewChild} from '@angular/core';
import {Subscription} from 'rxjs';
import {MonacoEditorConfig} from './config';

let loadedMonaco = false;
let loadPromise: Promise<void>;
declare const require: any;

export abstract class BaseEditor implements AfterViewInit, OnDestroy {
  @ViewChild('editorContainer') _editorContainer: ElementRef;

  @Output() init = new EventEmitter<any>();

  protected _editor: any;
  protected _options: any;
  protected _windowResizeSubscription: Subscription;

  constructor(protected config: MonacoEditorConfig) {
  }

  ngAfterViewInit(): void {
    if (loadedMonaco) {
      // Wait until monaco editor is available
      loadPromise.then(() => {
        this.initMonaco(this._options);
      });
    } else {
      loadedMonaco = true;
      loadPromise = new Promise<void>((resolve: any) => {
        const baseUrl = this.config.baseUrl || './assets';
        if (typeof ((<any>window).monaco) === 'object') {
          resolve();
          return;
        }
        const onGotAmdLoader: any = () => {
          // Load monaco
          (<any>window).require.config({paths: {'vs': `${baseUrl}/monaco/vs`}});
          (<any>window).require(['vs/editor/editor.main'], () => {
            if (typeof this.config.onMonacoLoad === 'function') {
              this.config.onMonacoLoad();
            }
            this.initMonaco(this._options);
            resolve();
          });
        };

        // Load AMD loader if necessary
        if (!(<any>window).require) {
          const loaderScript: HTMLScriptElement = document.createElement('script');
          loaderScript.type = 'text/javascript';
          loaderScript.src = `${baseUrl}/monaco/vs/loader.js`;
          loaderScript.addEventListener('load', onGotAmdLoader);
          document.body.appendChild(loaderScript);
        } else {
          onGotAmdLoader();
        }
      });
    }
  }

  protected abstract initMonaco(options: any): void;

  ngOnDestroy() {
    if (this._windowResizeSubscription) {
      this._windowResizeSubscription.unsubscribe();
    }
    if (this._editor) {
      this._editor.dispose();
      this._editor = undefined;
    }
  }
}


