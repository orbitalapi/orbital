import { AfterContentChecked, Directive, ElementRef, TemplateRef, ViewContainerRef } from '@angular/core';

@Directive({
    selector: '[fuseIfOnDom]'
})
export class FuseIfOnDomDirective implements AfterContentChecked
{
    isCreated: boolean;

    /**
     * Constructor
     *
     * @param {ElementRef} _elementRef
     * @param {TemplateRef<any>} _templateRef
     * @param {ViewContainerRef} _viewContainerRef
     */
    constructor(
        private _elementRef: ElementRef,
        private _templateRef: TemplateRef<any>,
        private _viewContainerRef: ViewContainerRef
    )
    {
        // Set the defaults
        this.isCreated = false;
    }

    // -----------------------------------------------------------------------------------------------------
    // @ Lifecycle hooks
    // -----------------------------------------------------------------------------------------------------

    /**
     * After content checked
     */
    ngAfterContentChecked(): void
    {
        if ( document.body.contains(this._elementRef.nativeElement) && !this.isCreated )
        {
            setTimeout(() => {
                this._viewContainerRef.createEmbeddedView(this._templateRef);
            }, 300);
            this.isCreated = true;
        }
        else if ( this.isCreated && !document.body.contains(this._elementRef.nativeElement) )
        {
            this._viewContainerRef.clear();
            this.isCreated = false;
        }
    }
}
