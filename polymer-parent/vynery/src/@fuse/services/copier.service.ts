/**
 * This class is based on the code in the following projects:
 * https://github.com/zenorocha/select
 * https://github.com/zenorocha/clipboard.js/
 *
 * Both released under MIT license - © Zeno Rocha
 */
import { Injectable } from '@angular/core';

@Injectable({
    providedIn: 'root'
})
export class FuseCopierService
{
    private textarea: HTMLTextAreaElement;

    /**
     * Copy the text value to the clipboard
     *
     * @param {string} text
     * @returns {boolean}
     */
    copyText(text: string): boolean
    {
        this.createTextareaAndSelect(text);

        const copySuccessful = document.execCommand('copy');
        this._removeFake();

        return copySuccessful;
    }

    /**
     * Creates a hidden textarea element, sets its value from `text` property,
     * and makes a selection on it.
     *
     * @param {string} text
     */
    private createTextareaAndSelect(text: string): void
    {
        // Create a fake element to hold the contents to copy
        this.textarea = document.createElement('textarea');

        // Prevent zooming on iOS
        this.textarea.style.fontSize = '12pt';

        // Hide the element
        this.textarea.classList.add('cdk-visually-hidden');

        // Move element to the same position vertically
        const yPosition = window.pageYOffset || document.documentElement.scrollTop;
        this.textarea.style.top = yPosition + 'px';

        this.textarea.setAttribute('readonly', '');
        this.textarea.value = text;

        document.body.appendChild(this.textarea);

        this.textarea.select();
        this.textarea.setSelectionRange(0, this.textarea.value.length);
    }

    /**
     * Remove the text area from the DOM
     *
     * @private
     */
    private _removeFake(): void
    {
        if ( this.textarea )
        {
            document.body.removeChild(this.textarea);
            this.textarea = null;
        }
    }
}
