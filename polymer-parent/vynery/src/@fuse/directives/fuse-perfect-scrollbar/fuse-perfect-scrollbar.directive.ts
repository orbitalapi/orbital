import { AfterViewInit, Directive, ElementRef, HostListener, Input, OnDestroy } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Platform } from '@angular/cdk/platform';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import PerfectScrollbar from 'perfect-scrollbar';
import * as _ from 'lodash';

import { FuseConfigService } from '@fuse/services/config.service';

@Directive({
    selector: '[fusePerfectScrollbar]'
})
export class FusePerfectScrollbarDirective implements AfterViewInit, OnDestroy
{
    isInitialized: boolean;
    isMobile: boolean;
    ps: PerfectScrollbar;

    // Private
    private _enabled: boolean | '';
    private _debouncedUpdate: any;
    private _options: any;
    private _unsubscribeAll: Subject<any>;

    /**
     * Constructor
     *
     * @param {ElementRef} elementRef
     * @param {FuseConfigService} _fuseConfigService
     * @param {Platform} _platform
     * @param {Router} _router
     */
    constructor(
        public elementRef: ElementRef,
        private _fuseConfigService: FuseConfigService,
        private _platform: Platform,
        private _router: Router
    )
    {
        // Set the defaults
        this.isInitialized = false;
        this.isMobile = false;

        // Set the private defaults
        this._enabled = false;
        this._debouncedUpdate = _.debounce(this.update, 150);
        this._options = {
            updateOnRouteChange: false
        };
        this._unsubscribeAll = new Subject();
    }

    // -----------------------------------------------------------------------------------------------------
    // @ Accessors
    // -----------------------------------------------------------------------------------------------------

    /**
     * Perfect Scrollbar options
     *
     * @param value
     */
    @Input()
    set fusePerfectScrollbarOptions(value)
    {
        // Merge the options
        this._options = _.merge({}, this._options, value);
    }

    get fusePerfectScrollbarOptions(): any
    {
        // Return the options
        return this._options;
    }

    /**
     * Is enabled
     *
     * @param {boolean | ""} value
     */
    @Input('fusePerfectScrollbar')
    set enabled(value: boolean | '')
    {
        // If nothing is provided with the directive (empty string),
        // we will take that as a true
        if ( value === '' )
        {
            value = true;
        }

        // Return, if both values are the same
        if ( this.enabled === value )
        {
            return;
        }

        // Store the value
        this._enabled = value;

        // If enabled...
        if ( this.enabled )
        {
            // Init the directive
            this._init();
        }
        else
        {
            // Otherwise destroy it
            this._destroy();
        }
    }

    get enabled(): boolean | ''
    {
        // Return the enabled status
        return this._enabled;
    }

    // -----------------------------------------------------------------------------------------------------
    // @ Lifecycle hooks
    // -----------------------------------------------------------------------------------------------------

    /**
     * After view init
     */
    ngAfterViewInit(): void
    {
        // Check if scrollbars enabled or not from the main config
        this._fuseConfigService.config
            .pipe(takeUntil(this._unsubscribeAll))
            .subscribe(
                (settings) => {
                    this.enabled = settings.customScrollbars;
                }
            );

        // Scroll to the top on every route change
        if ( this.fusePerfectScrollbarOptions.updateOnRouteChange )
        {
            this._router.events
                .pipe(
                    takeUntil(this._unsubscribeAll),
                    filter(event => event instanceof NavigationEnd)
                )
                .subscribe(() => {
                    setTimeout(() => {
                        this.scrollToTop();
                        this.update();
                    }, 0);
                });
        }
    }

    /**
     * On destroy
     */
    ngOnDestroy(): void
    {
        this._destroy();

        // Unsubscribe from all subscriptions
        this._unsubscribeAll.next();
        this._unsubscribeAll.complete();
    }

    // -----------------------------------------------------------------------------------------------------
    // @ Private methods
    // -----------------------------------------------------------------------------------------------------

    /**
     * Initialize
     *
     * @private
     */
    _init(): void
    {
        // Return, if already initialized
        if ( this.isInitialized )
        {
            return;
        }

        // Check if is mobile
        if ( this._platform.ANDROID || this._platform.IOS )
        {
            this.isMobile = true;
        }

        // Return if it's mobile
        if ( this.isMobile )
        {
            // Return...
            return;
        }

        // Set as initialized
        this.isInitialized = true;

        // Initialize the perfect-scrollbar
        this.ps = new PerfectScrollbar(this.elementRef.nativeElement, {
            ...this.fusePerfectScrollbarOptions
        });
    }

    /**
     * Destroy
     *
     * @private
     */
    _destroy(): void
    {
        if ( !this.isInitialized || !this.ps )
        {
            return;
        }

        // Destroy the perfect-scrollbar
        this.ps.destroy();

        // Clean up
        this.ps = null;
        this.isInitialized = false;
    }

    /**
     * Update scrollbars on window resize
     *
     * @private
     */
    @HostListener('window:resize')
    _updateOnResize(): void
    {
        this._debouncedUpdate();
    }

    // -----------------------------------------------------------------------------------------------------
    // @ Public methods
    // -----------------------------------------------------------------------------------------------------

    /**
     * Document click
     *
     * @param {Event} event
     */
    @HostListener('document:click', ['$event'])
    documentClick(event: Event): void
    {
        if ( !this.isInitialized || !this.ps )
        {
            return;
        }

        // Update the scrollbar on document click..
        // This isn't the most elegant solution but there is no other way
        // of knowing when the contents of the scrollable container changes.
        // Therefore, we update scrollbars on every document click.
        this.ps.update();
    }

    /**
     * Update the scrollbar
     */
    update(): void
    {
        if ( !this.isInitialized )
        {
            return;
        }

        // Update the perfect-scrollbar
        this.ps.update();
    }

    /**
     * Destroy the scrollbar
     */
    destroy(): void
    {
        this.ngOnDestroy();
    }

    /**
     * Scroll to X
     *
     * @param {number} x
     * @param {number} speed
     */
    scrollToX(x: number, speed?: number): void
    {
        this.animateScrolling('scrollLeft', x, speed);
    }

    /**
     * Scroll to Y
     *
     * @param {number} y
     * @param {number} speed
     */
    scrollToY(y: number, speed?: number): void
    {
        this.animateScrolling('scrollTop', y, speed);
    }

    /**
     * Scroll to top
     *
     * @param {number} offset
     * @param {number} speed
     */
    scrollToTop(offset?: number, speed?: number): void
    {
        this.animateScrolling('scrollTop', (offset || 0), speed);
    }

    /**
     * Scroll to left
     *
     * @param {number} offset
     * @param {number} speed
     */
    scrollToLeft(offset?: number, speed?: number): void
    {
        this.animateScrolling('scrollLeft', (offset || 0), speed);
    }

    /**
     * Scroll to right
     *
     * @param {number} offset
     * @param {number} speed
     */
    scrollToRight(offset?: number, speed?: number): void
    {
        const width = this.elementRef.nativeElement.scrollWidth;

        this.animateScrolling('scrollLeft', width - (offset || 0), speed);
    }

    /**
     * Scroll to bottom
     *
     * @param {number} offset
     * @param {number} speed
     */
    scrollToBottom(offset?: number, speed?: number): void
    {
        const height = this.elementRef.nativeElement.scrollHeight;

        this.animateScrolling('scrollTop', height - (offset || 0), speed);
    }

    /**
     * Animate scrolling
     *
     * @param {string} target
     * @param {number} value
     * @param {number} speed
     */
    animateScrolling(target: string, value: number, speed?: number): void
    {
        if ( !speed )
        {
            this.elementRef.nativeElement[target] = value;

            // PS has weird event sending order, this is a workaround for that
            this.update();
            this.update();
        }
        else if ( value !== this.elementRef.nativeElement[target] )
        {
            let newValue = 0;
            let scrollCount = 0;

            let oldTimestamp = performance.now();
            let oldValue = this.elementRef.nativeElement[target];

            const cosParameter = (oldValue - value) / 2;

            const step = (newTimestamp) => {
                scrollCount += Math.PI / (speed / (newTimestamp - oldTimestamp));

                newValue = Math.round(value + cosParameter + cosParameter * Math.cos(scrollCount));

                // Only continue animation if scroll position has not changed
                if ( this.elementRef.nativeElement[target] === oldValue )
                {
                    if ( scrollCount >= Math.PI )
                    {
                        this.elementRef.nativeElement[target] = value;

                        // PS has weird event sending order, this is a workaround for that
                        this.update();

                        this.update();
                    }
                    else
                    {
                        this.elementRef.nativeElement[target] = oldValue = newValue;

                        oldTimestamp = newTimestamp;

                        window.requestAnimationFrame(step);
                    }
                }
            };

            window.requestAnimationFrame(step);
        }
    }
}
