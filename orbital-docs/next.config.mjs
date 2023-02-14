// const withMarkdoc = require('@markdoc/next.js')

import remarkFrontmatter from 'remark-frontmatter';
import NextMdx from '@next/mdx';
import recmaNextjsStaticProps from "recma-nextjs-static-props";
import remarkGfm from "remark-gfm";
import {rehypePlugins} from "./mdx/rehype.mjs";

const withMDX = NextMdx({
   extension: /\.mdx?$/,
   options: {
      // If you use remark-gfm, you'll need to use next.config.mjs
      // as the package is ESM only
      // https://github.com/remarkjs/remark-gfm#install
      remarkPlugins: [remarkFrontmatter, remarkGfm],
      rehypePlugins,
      recmaPlugins: [recmaNextjsStaticProps],
      // If you use `MDXProvider`, uncomment the following line.
      providerImportSource: "@mdx-js/react",
   },
});

/** @type {import('next').NextConfig} */
const nextConfig = {
   reactStrictMode: true,
   pageExtensions: ['js', 'jsx', 'md', 'ts', 'tsx', 'mdx'],
   experimental: {
      scrollRestoration: true,
   },
}

export default withMDX(nextConfig);
