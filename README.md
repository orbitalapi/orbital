# Orbitalhq.com (website and blog)

This is our website and blogs.

## Tech stack

The project is built on NextJS and TailwindCSS.

```bash
npm install
```

Next, run the development server:

```bash
npm run dev
```

## Blogs

Blogs are authored in MDX.

Create a directory in `src/pages/blog` with the  (`YYYY-MM-DD`) and title (or something similar) of your blog post.

eg:

```
src/pages/blog/2023-01-16-winning-rap-battles-against-gran
```

Within there, add a single mdx called `index.mdx`

### Authors

Add an entry to `src/authors.js`, along with your avatar in `img/authors/YourPrettyFace.jpg`.

### Blog metadata

NextJS's mdx support doesn't currently work well with traditional frontmatter (ie., I couldn't get it to work when I
tried), so we're
using ESM metadata.

Here's a sample of blog metadata:

```js
import {martyPitt} from '@/authors'
// See below about creating a card
import card from './card.jpg'

export const meta = {
  title: 'How to win a rap battle against your gran',
  description: `Granny has mad rhymes, and she schooling you each time you step to the mic.  Let's take the game back.`,
  date: '2023-01-16T19:00:00.000Z',
  authors: [martyPitt],
  // This is the card that's shown when sharing on socials
  image: card,
}
```

### Generating a share card

TODO

### Useful components

#### Images

* Place images in the directory of your blog post.
* Standard markdown format is fine for normal images.
* Use `ImageWithCaption` for an image that's wider, needs a caption, or is a share from Voyager:

```js
import {ImageWithCaption} from '@/components/ImageWithCaption'
import rapBattleBasics from './rap-battle-basics.png'

// Use wide
<
ImageWithCaption
src = {rapBattleBasics.src}
caption = {`She may be old, but her raps are fresh.`
}
wide // Use for larger images you want to break out of the default column format.
voyagerLink = {`DCb8cYYKmj`
} // Paste the code from the share link (just the id, not the whole share link)
/>
```

#### Code Snippets

* Standard markdown snippets are fine for single-file snippets
* Add the language after backticks.  `taxi` is a supported language
  * (See `src/utils/taxiPrismLanguage.js` for the language definition)
* Use `SnippetGroup` if you want multiple tabs:

<!-- This is in a code block, since nested back-ticks would be confusing -->
<code>
import { SnippetGroup } from '@/components/SnippetGroup'

<SnippetGroup>

```taxi Service.taxi
model Foo { ... }
```

```yaml Customer OpenAPI Spec
# An extract of the CustomerApi OpenAPI spec:
components:
  schemas:
    Customer:
      ...
```

</SnippetGroup>

</code>

#### Mermaid Diagrams

Mermaid diagrams can use `<OrbitalMermaid />`. This is a small wrapper around mdx-mermaid,
which handles theme detection on load.

Note: Theme changes aren't re-rendering mermaid. This appears to be a known issue in mermaid, without a
workaround. [0](https://github.com/mermaid-js/mermaid/issues/1544) [1](https://github.com/mermaid-js/mermaid/issues/1945)

```
import OrbitalMermaid from "@/components/OrbitalMermaid";


<OrbitalMermaid chart={`sequenceDiagram
    Orbital->>CustomerApi: getCustomer
    Orbital->>CardsApi: getBalance
    `} />
```
