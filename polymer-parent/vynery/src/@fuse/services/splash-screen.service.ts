import { Inject, Injectable } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { animate, AnimationBuilder, AnimationPlayer, style } from '@angular/animations';
import { NavigationEnd, Router } from '@angular/router';

@Injectable({
    providedIn: 'root'
})
export class FuseSplashScreenService
{
    splashScreenEl: any;
    player: AnimationPlayer;

    /**
     * Constructor
     *
     * @param {AnimationBuilder} _animationBuilder
     * @param _document
     * @param {Router} _router
     */
    constructor(
        private _animationBuilder: AnimationBuilder,
        @Inject(DOCUMENT) private _document: any,
        private _router: Router
    )
    {
        // Initialize
        this._init();
    }

    // -----------------------------------------------------------------------------------------------------
    // @ Private methods
    // -----------------------------------------------------------------------------------------------------

    /**
     * Initialize
     *
     * @private
     */
    private _init(): void
    {
        // Get the splash screen element
        this.splashScreenEl = this._document.body.querySelector('#fuse-splash-screen');

        // If the splash screen element exists...
        if ( this.splashScreenEl )
        {
            // Hide it on the first NavigationEnd event
            const hideOnLoad = this._router.events.subscribe((event) => {
                    if ( event instanceof NavigationEnd )
                    {
                        setTimeout(() => {
                            this.hide();

                            // Unsubscribe from this event so it
                            // won't get triggered again
                            hideOnLoad.unsubscribe();
                        }, 0);
                    }
                }
            );
        }
    }

    // -----------------------------------------------------------------------------------------------------
    // @ Public methods
    // -----------------------------------------------------------------------------------------------------

    /**
     * Show the splash screen
     */
    show(): void
    {
        this.player =
            this._animationBuilder
                .build([
                    style({
                        opacity: '0',
                        zIndex : '99999'
                    }),
                    animate('400ms ease', style({opacity: '1'}))
                ]).create(this.splashScreenEl);

        setTimeout(() => {
            this.player.play();
        }, 0);
    }

    /**
     * Hide the splash screen
     */
    hide(): void
    {
        this.player =
            this._animationBuilder
                .build([
                    style({opacity: '1'}),
                    animate('400ms ease', style({
                        opacity: '0',
                        zIndex : '-10'
                    }))
                ]).create(this.splashScreenEl);

        setTimeout(() => {
            this.player.play();
        }, 0);
    }
}
