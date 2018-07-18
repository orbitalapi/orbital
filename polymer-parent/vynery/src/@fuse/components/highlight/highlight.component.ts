import { Component, ContentChild, ElementRef, Input, OnDestroy, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import * as Prism from 'prismjs/prism';
import '@fuse/components/highlight/prism-languages';

@Component({
    selector : 'fuse-highlight',
    template : '',
    styleUrls: ['./highlight.component.scss']
})
export class FuseHighlightComponent implements OnInit, OnDestroy
{
    // Source
    @ContentChild('source')
    source: ElementRef;

    // Lang
    @Input('lang')
    lang: string;

    // Path
    @Input('path')
    path: string;

    // Private
    private _unsubscribeAll: Subject<any>;

    /**
     * Constructor
     *
     * @param {ElementRef} _elementRef
     * @param {HttpClient} _httpClient
     */
    constructor(
        private _elementRef: ElementRef,
        private _httpClient: HttpClient
    )
    {
        // Set the private defaults
        this._unsubscribeAll = new Subject();
    }

    // -----------------------------------------------------------------------------------------------------
    // @ Lifecycle hooks
    // -----------------------------------------------------------------------------------------------------

    /**
     * On init
     */
    ngOnInit(): void
    {
        // If there is no language defined, return...
        if ( !this.lang )
        {
            return;
        }

        // If the path is defined...
        if ( this.path )
        {
            // Get the source
            this._httpClient.get(this.path, {responseType: 'text'})
                .pipe(takeUntil(this._unsubscribeAll))
                .subscribe((response) => {

                    // Highlight it
                    this.highlight(response);
                });
        }

        // If the path is not defined and the source element exists...
        if ( !this.path && this.source )
        {
            // Highlight it
            this.highlight(this.source.nativeElement.value);
        }
    }

    /**
     * On destroy
     */
    ngOnDestroy(): void
    {
        // Unsubscribe from all subscriptions
        this._unsubscribeAll.next();
        this._unsubscribeAll.complete();
    }

    // -----------------------------------------------------------------------------------------------------
    // @ Public methods
    // -----------------------------------------------------------------------------------------------------

    /**
     * Highlight the given source code
     *
     * @param sourceCode
     */
    highlight(sourceCode): void
    {
        // Split the source into lines
        const sourceLines = sourceCode.split('\n');

        // Remove the first and the last line of the source
        // code if they are blank lines. This way, the html
        // can be formatted properly while using fuse-highlight
        // component
        if ( !sourceLines[0].trim() )
        {
            sourceLines.shift();
        }

        if ( !sourceLines[sourceLines.length - 1].trim() )
        {
            sourceLines.pop();
        }

        // Find the first non-whitespace char index in
        // the first line of the source code
        const indexOfFirstChar = sourceLines[0].search(/\S|$/);

        // Generate the trimmed source
        let source = '';

        // Iterate through all the lines
        sourceLines.forEach((line, index) => {

            // Trim the beginning white space depending on the index
            // and concat the source code
            source = source + line.substr(indexOfFirstChar, line.length);

            // If it's not the last line...
            if ( index !== sourceLines.length - 1 )
            {
                // Add a line break at the end
                source = source + '\n';
            }
        });

        // Generate the highlighted code
        const highlightedCode = Prism.highlight(source, Prism.languages[this.lang]);

        // Replace the innerHTML of the component with the highlighted code
        this._elementRef.nativeElement.innerHTML =
            '<pre><code class="highlight language-' + this.lang + '">' + highlightedCode + '</code></pre>';
    }
}

