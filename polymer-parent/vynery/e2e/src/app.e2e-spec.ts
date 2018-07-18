import { Fuse2Page } from './app.po';

describe('Fuse2 App', () => {
    let page: Fuse2Page;

    beforeEach(() => {
        page = new Fuse2Page();
    });

    it('should display welcome message', () => {
        page.navigateTo();
        expect(page.getParagraphText()).toEqual('Welcome to Fuse2!');
    });
});
