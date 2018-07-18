import { Component, EventEmitter, Input, OnChanges, Output, ViewEncapsulation } from '@angular/core';

import { fuseAnimations } from '@fuse/animations';
import { MatColors } from '@fuse/mat-colors';

@Component({
    selector     : 'fuse-material-color-picker',
    templateUrl  : './material-color-picker.component.html',
    styleUrls    : ['./material-color-picker.component.scss'],
    animations   : fuseAnimations,
    encapsulation: ViewEncapsulation.None
})
export class FuseMaterialColorPickerComponent implements OnChanges
{
    colors: any;
    hues: string[];
    selectedColor: any;
    view: string;

    @Input()
    selectedPalette: string;

    @Input()
    selectedHue: string;

    @Input()
    selectedFg: string;

    @Input()
    value: any;

    @Output()
    onValueChange: EventEmitter<any>;

    @Output()
    selectedPaletteChange: EventEmitter<any>;

    @Output()
    selectedHueChange: EventEmitter<any>;

    @Output()
    selectedClassChange: EventEmitter<any>;

    @Output()
    selectedBgChange: EventEmitter<any>;

    @Output()
    selectedFgChange: EventEmitter<any>;

    // Private
    _selectedClass: string;
    _selectedBg: string;

    /**
     * Constructor
     */
    constructor()
    {
        // Set the defaults
        this.colors = MatColors.all;
        this.hues = ['50', '100', '200', '300', '400', '500', '600', '700', '800', '900', 'A100', 'A200', 'A400', 'A700'];
        this.selectedFg = '';
        this.selectedHue = '';
        this.selectedPalette = '';
        this.view = 'palettes';

        this.onValueChange = new EventEmitter();
        this.selectedPaletteChange = new EventEmitter();
        this.selectedHueChange = new EventEmitter();
        this.selectedClassChange = new EventEmitter();
        this.selectedBgChange = new EventEmitter();
        this.selectedFgChange = new EventEmitter();

        // Set the private defaults
        this._selectedClass = '';
        this._selectedBg = '';
    }

    // -----------------------------------------------------------------------------------------------------
    // @ Accessors
    // -----------------------------------------------------------------------------------------------------

    /**
     * Selected class
     *
     * @param value
     */
    @Input()
    set selectedClass(value)
    {
        if ( value && value !== '' && this._selectedClass !== value )
        {
            const color = value.split('-');
            if ( color.length >= 5 )
            {
                this.selectedPalette = color[1] + '-' + color[2];
                this.selectedHue = color[3];
            }
            else
            {
                this.selectedPalette = color[1];
                this.selectedHue = color[2];
            }
        }
        this._selectedClass = value;
    }

    get selectedClass(): string
    {
        return this._selectedClass;
    }

    /**
     * Selected bg
     *
     * @param value
     */
    @Input()
    set selectedBg(value)
    {
        if ( value && value !== '' && this._selectedBg !== value )
        {
            for ( const palette in this.colors )
            {
                if ( !this.colors.hasOwnProperty(palette) )
                {
                    continue;
                }

                for ( const hue of this.hues )
                {
                    if ( this.colors[palette][hue] === value )
                    {
                        this.selectedPalette = palette;
                        this.selectedHue = hue;
                        break;
                    }
                }
            }
        }
        this._selectedBg = value;
    }

    get selectedBg(): string
    {
        return this._selectedBg;
    }

    // -----------------------------------------------------------------------------------------------------
    // @ Lifecycle hooks
    // -----------------------------------------------------------------------------------------------------

    /**
     * On changes
     *
     * @param changes
     */
    ngOnChanges(changes: any): void
    {
        if ( changes.selectedBg && changes.selectedBg.currentValue === '' ||
            changes.selectedClass && changes.selectedClass.currentValue === '' ||
            changes.selectedPalette && changes.selectedPalette.currentValue === '' )
        {
            this.removeColor();
            return;
        }
        if ( changes.selectedPalette || changes.selectedHue || changes.selectedClass || changes.selectedBg )
        {
            this.updateSelectedColor();
        }
    }

    // -----------------------------------------------------------------------------------------------------
    // @ Public methods
    // -----------------------------------------------------------------------------------------------------

    /**
     * Select palette
     *
     * @param palette
     */
    selectPalette(palette): void
    {
        this.selectedPalette = palette;
        this.updateSelectedColor();
        this.view = 'hues';
    }

    /**
     * Select hue
     *
     * @param hue
     */
    selectHue(hue): void
    {
        this.selectedHue = hue;
        this.updateSelectedColor();
    }

    /**
     * Remove color
     */
    removeColor(): void
    {
        this.selectedPalette = '';
        this.selectedHue = '';
        this.updateSelectedColor();
        this.view = 'palettes';
    }

    /**
     * Update selected color
     */
    updateSelectedColor(): void
    {
        setTimeout(() => {

            if ( this.selectedColor && this.selectedPalette === this.selectedColor.palette && this.selectedHue === this.selectedColor.hue )
            {
                return;
            }

            if ( this.selectedPalette !== '' && this.selectedHue !== '' )
            {
                this.selectedBg = MatColors.getColor(this.selectedPalette)[this.selectedHue];
                this.selectedFg = MatColors.getColor(this.selectedPalette).contrast[this.selectedHue];
                this.selectedClass = 'mat-' + this.selectedPalette + '-' + this.selectedHue + '-bg';
            }
            else
            {
                this.selectedBg = '';
                this.selectedFg = '';
            }

            this.selectedColor = {
                palette: this.selectedPalette,
                hue    : this.selectedHue,
                class  : this.selectedClass,
                bg     : this.selectedBg,
                fg     : this.selectedFg
            };

            this.selectedPaletteChange.emit(this.selectedPalette);
            this.selectedHueChange.emit(this.selectedHue);
            this.selectedClassChange.emit(this.selectedClass);
            this.selectedBgChange.emit(this.selectedBg);
            this.selectedFgChange.emit(this.selectedFg);

            this.value = this.selectedColor;
            this.onValueChange.emit(this.selectedColor);
        });
    }

    /**
     * Go back to palette selection
     */
    backToPaletteSelection(): void
    {
        this.view = 'palettes';
    }

    /**
     * On menu open
     */
    onMenuOpen(): void
    {
        if ( this.selectedPalette === '' )
        {
            this.view = 'palettes';
        }
        else
        {
            this.view = 'hues';
        }
    }
}
