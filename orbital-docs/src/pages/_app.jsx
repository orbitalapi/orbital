import Head from 'next/head'
import {slugifyWithCounter} from '@sindresorhus/slugify'

import {Layout} from '@/components/Layout'

import 'focus-visible'
import '@/styles/tailwind.css'
import {MDXProvider} from "@mdx-js/react";

function getNodeText(node) {
   let text = ''
   for (let child of node.children ?? []) {
      if (typeof child === 'string') {
         text += child
      }
      text += getNodeText(child)
   }
   return text
}

// getHeadings credit: Josh W. Comeau
export async function getHeadings(source) {
   // Get each line individually, and filter out anything that
   // isn't a heading.
   const headingLines = source.split("\n").filter((line) => {
      return line.match(/^###*\s/);
   });

   // Transform the string '## Some text' into an object
   // with the shape '{ text: 'Some text', level: 2 }'
   return headingLines.map((raw) => {
      const text = raw.replace(/^###*\s/, "");
      // I only care about h2 and h3.
      // If I wanted more levels, I'd need to count the
      // number of #s.
      const level = raw.slice(0, 3) === "###" ? 3 : 2;

      return {text, level};
   });
}

function collectHeadings(nodes, slugify = slugifyWithCounter()) {
   let sections = []

   for (let node of nodes) {
      if (node.name === 'h2' || node.name === 'h3') {
         let title = getNodeText(node)
         if (title) {
            let id = slugify(title)
            node.attributes.id = id
            if (node.name === 'h3') {
               if (!sections[sections.length - 1]) {
                  throw new Error(
                     'Cannot add `h3` to table of contents without a preceding `h2`'
                  )
               }
               sections[sections.length - 1].children.push({
                  ...node.attributes,
                  title,
               })
            } else {
               sections.push({...node.attributes, title, children: []})
            }
         }
      }

      sections.push(...collectHeadings(node.children ?? [], slugify))
   }

   return sections
}

export default function App({Component, pageProps}) {
   let title = pageProps.meta?.title

   let pageTitle = pageProps.meta?.pageTitle || pageProps.meta?.title
   let description = pageProps.meta?.description
   let tableOfContents = pageProps.sections;

   return (
      <>
         <Head>
            <title>{pageTitle}</title>
            {description && <meta name="description" content={description}/>}
         </Head>
         <MDXProvider>
            <Layout title={title} tableOfContents={tableOfContents}>
               <Component {...pageProps} />
            </Layout>
         </MDXProvider>
      </>
   )
}
