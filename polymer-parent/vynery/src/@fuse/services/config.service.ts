import { Inject, Injectable, InjectionToken } from '@angular/core';
import { NavigationStart, Router } from '@angular/router';
import { Platform } from '@angular/cdk/platform';
import { BehaviorSubject, Observable } from 'rxjs';
import { filter } from 'rxjs/operators';
import * as _ from 'lodash';

// Create the injection token for the custom settings
export const FUSE_CONFIG = new InjectionToken('fuseCustomConfig');

@Injectable({
    providedIn: 'root'
})
export class FuseConfigService
{
    // Private
    private _configSubject: BehaviorSubject<any>;
    private readonly _defaultConfig: any;

    /**
     * Constructor
     *
     * @param {Platform} _platform
     * @param {Router} _router
     * @param _config
     */
    constructor(
        private _platform: Platform,
        private _router: Router,
        @Inject(FUSE_CONFIG) private _config
    )
    {
        // Set the default config from the user provided config (from forRoot)
        this._defaultConfig = _config;

        // Initialize the service
        this._init();
    }

    // -----------------------------------------------------------------------------------------------------
    // @ Accessors
    // -----------------------------------------------------------------------------------------------------

    /**
     * Set and get the config
     */
    set config(value)
    {
        // Get the value from the behavior subject
        let config = this._configSubject.getValue();

        // Merge the new config
        config = _.merge({}, config, value);

        // Notify the observers
        this._configSubject.next(config);
    }

    get config(): any | Observable<any>
    {
        return this._configSubject.asObservable();
    }

    /**
     * Get default config
     *
     * @returns {any}
     */
    get defaultConfig(): any
    {
        return this._defaultConfig;
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
        /**
         * Disable custom scrollbars if browser is mobile
         */
        if ( this._platform.ANDROID || this._platform.IOS )
        {
            this._defaultConfig.customScrollbars = false;
        }

        // Set the config from the default config
        this._configSubject = new BehaviorSubject(_.cloneDeep(this._defaultConfig));

        // Reload the default config on every navigation start if
        // the current config is different from the default one
        this._router.events
            .pipe(filter(event => event instanceof NavigationStart))
            .subscribe(() => {
                if ( !_.isEqual(this._configSubject.getValue(), this._defaultConfig) )
                {
                    // Clone the default config
                    const config = _.cloneDeep(this._defaultConfig);

                    // Set the config
                    this._configSubject.next(config);
                }
            });
    }

    // -----------------------------------------------------------------------------------------------------
    // @ Public methods
    // -----------------------------------------------------------------------------------------------------

    /**
     * Set config
     *
     * @param value
     * @param {{emitEvent: boolean}} opts
     */
    setConfig(value, opts = {emitEvent: true}): void
    {
        // Get the value from the behavior subject
        let config = this._configSubject.getValue();

        // Merge the new config
        config = _.merge({}, config, value);

        // If emitEvent option is true...
        if ( opts.emitEvent === true )
        {
            // Notify the observers
            this._configSubject.next(config);
        }
    }

    /**
     * Get config
     *
     * @returns {Observable<any>}
     */
    getConfig(): Observable<any>
    {
        return this._configSubject.asObservable();
    }

    /**
     * Reset to the default config
     */
    resetToDefaults(): void
    {
        // Set the config from the default config
        this._configSubject.next(_.cloneDeep(this._defaultConfig));
    }
}

