import { Page } from '@playwright/test';

export async function selectTab(page: Page, text: string, selectorPrefix = ''): Promise<void> {
   await page.click(`${selectorPrefix} tui-tabs button:has-text("${text}")`);
}

export async function openDropdown(page: Page, text: string, selectorPrefix = ''): Promise<void> {
   await page.click(`${selectorPrefix} tui-wrapper:has-text("${text}")`);
}

export async function selectDropdownOption(page: Page, text: string, prefix = ''): Promise<void> {
   await page.click(`${prefix} button:has-text("${text}")`);
}

export async function clickButton(page: Page, text: string, prefix = ''): Promise<void> {
   await page.click(`${prefix} button:has-text("${text}")`);
}

export async function fillValue(page: Page, label: string, value: string, prefix = ''): Promise<void> {
   await page.locator(`${prefix} tui-wrapper:has-text("${label}") input`).fill(value);
}

export async function selectOnAutoComplete(page: Page, label: string, value: string, prefix = ''): Promise<void> {
   await page.locator(`${prefix} app-type-autocomplete:has-text("${label}") input`).fill(value);
   await page.click(`${prefix} .mat-option:has-text("${value}")`);
}

export async function openAccordion(page: Page, text: string, selectorPrefix = ''): Promise<void> {
   await page.click(`${selectorPrefix} tui-accordion-item .header:has-text("${text}")`);
}

export async function selectTreeItem(page: Page, text: string, selectorPrefix = ''): Promise<void> {
   await page.click(`${selectorPrefix} tui-tree-item-content:has-text("${text}")`);
}
