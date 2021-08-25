# Documenting Vyne

## Paying down our documentation-debt
Our docs need work.  This is best tackled incrementally, by everyone.

When adding new features, be sure to add docs.  When you're editing docs, feel free 
to fix up things that are inconsistent, out-of-style, or incorrect.

## Tone of voice
Vyne's tone is laid-back and conversational.  It's ok (encouraged) to make careful use of humour.

We need to pick a style guide for the language we use in our docs.  As a starter, we'll use [Googles](https://developers.google.com/style), but 
options are open for a better alternative.

## Provide simple to understand examples
Examples should be provided in the context of an easy-to-understand domain.  
 * ✅ Movies
 * ✅ Retail shopping
 * ❌ Finance

Never use client-specific terminology in our docs.

If adding an example to the docs, try to also add an associated unit test in the code base
that tests the example. 

Link back to the relevant section of the docs in the test, should they need updating.

## Images
Use images where possible.  However, be consistent with the style.  

We have a library of documentation images that form a consistent style [here](https://app.diagrams.net/#G1yMMomp9udh7ZsTbkATE6Xji0Dp0GnOUs).

When editing that file, be additive, but not destructive.


# Running the docs locally
Our docs are built on Gatsby, using the [Apollo GraphQL])(https://github.com/apollographql/gatsby-theme-apollo) starter theme.

```bash
cd docs/
npm install
./node_modules/.bin/gatsby develop
```
