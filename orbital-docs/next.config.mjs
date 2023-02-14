// const withMarkdoc = require('@markdoc/next.js')
import {createLoader} from 'simple-functional-loader';
import remarkFrontmatter from 'remark-frontmatter';
import NextMdx from '@next/mdx';
import recmaNextjsStaticProps from "recma-nextjs-static-props";
import remarkGfm from "remark-gfm";
import {rehypePlugins} from "./mdx/rehype-plugins/rehype.mjs";
import {withSyntaxHighlighting} from "./mdx/remark-plugins/withSyntaxHighlighting.js";
import {highlightCode, normalizeTokens, simplifyToken} from "./mdx/utils.js";
import * as Prism from 'prismjs';

const withMDX = NextMdx({
   extension: /\.mdx?$/,
   options: {
      // If you use remark-gfm, you'll need to use next.config.mjs
      // as the package is ESM only
      // https://github.com/remarkjs/remark-gfm#install
      remarkPlugins: [remarkFrontmatter, remarkGfm, withSyntaxHighlighting],
      rehypePlugins,
      recmaPlugins: [recmaNextjsStaticProps],
      // If you use `MDXProvider`, uncomment the following line.
      providerImportSource: "@mdx-js/react",
   },
});

function fixSelectorEscapeTokens(tokens) {
   for (let token of tokens) {
      if (typeof token === 'string') continue
      if (token.type !== 'selector') continue
      for (let i = 0; i < token.content.length; i++) {
         if (token.content[i] === '\\' && token.content[i - 1]?.type === 'class') {
            token.content[i] = new Prism.Token('punctuation', token.content[i])
            token.content[i + 1].type = 'class'
         }
      }
   }
}

/** @type {import('next').NextConfig} */
const nextConfig = {
   reactStrictMode: true,
   pageExtensions: ['js', 'jsx', 'md', 'ts', 'tsx', 'mdx'],
   experimental: {
      scrollRestoration: true,
   },
   webpack(config,options) {
      config.module.rules.push({
         resourceQuery: /highlight/,
         use: [
            options.defaultLoaders.babel,
            createLoader(function (source) {
               let lang =
                  new URLSearchParams(this.resourceQuery).get('highlight') ||
                  this.resourcePath.split('.').pop()
               let isDiff = lang.startsWith('diff-')
               let prismLang = isDiff ? lang.substr(5) : lang
               let grammar = Prism.languages[isDiff ? 'diff' : prismLang]
               let tokens = Prism.tokenize(source, grammar, lang)

               if (lang === 'css') {
                  fixSelectorEscapeTokens(tokens)
               }

               return `
            export const tokens = ${JSON.stringify(tokens.map(simplifyToken))}
            export const lines = ${JSON.stringify(normalizeTokens(tokens))}
            export const code = ${JSON.stringify(source)}
            export const highlightedCode = ${JSON.stringify(highlightCode(source, lang))}
          `
            }),
         ],
      })
      return config;
   }
}

export default withMDX(nextConfig);
