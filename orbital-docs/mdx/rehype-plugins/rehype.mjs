import * as acorn from 'acorn'
import {toString} from 'mdast-util-to-string'
import {slugifyWithCounter} from '@sindresorhus/slugify'
import {visit} from 'unist-util-visit'
import rehypeMdxTitle from 'rehype-mdx-title'

function rehypeSlugify() {
   return (tree) => {
      let slugify = slugifyWithCounter()
      visit(tree, 'element', (node) => {
         if ((node.tagName === 'h2' || node.tagName === 'h3') && !node.properties.id) {
            node.properties.id = slugify(toString(node))
         }
      })
   }
}

function rehypeAddMDXExports(getExports) {
   return (tree) => {
      let exports = Object.entries(getExports(tree))

      for (let [name, value] of exports) {
         for (let node of tree.children) {
            if (
               node.type === 'mdxjsEsm' &&
               new RegExp(`export\\s+const\\s+${name}\\s*=`).test(node.value)
            ) {
               return
            }
         }

         let exportStr = `export const ${name} = ${value}`

         tree.children.push({
            type: 'mdxjsEsm',
            value: exportStr,
            data: {
               estree: acorn.parse(exportStr, {
                  sourceType: 'module',
                  ecmaVersion: 'latest',
               }),
            },
         })
      }
   }
}

function getSections(node) {
   let sections = []

   for (let child of node.children ?? []) {
      if (child.type === 'element' && child.tagName === 'h2') {
         sections.push({
            title: toString(child),
            id: child.properties.id,
            children: []
         })
      } else if (child.type === 'element' && child.tagName === 'h3') {
         if (sections.length === 0) {
            console.error(`Found an h3 element without an h2, somewhere near ${toString(child)}`)
         } else {
            sections[sections.length - 1].children.push({
               title: toString(child),
               id: child.properties.id,
            })
         }
      } else if (child.children) {
         sections.push(...getSections(child))
      }
   }


   return sections.map(section => JSON.stringify(section));
}


export const rehypePlugins = [
   // rehypeHighlight,
   // mdxAnnotations.rehype,
   // rehypeParseCodeBlocks,
   // rehypeShiki,
   rehypeSlugify,
   rehypeMdxTitle,
   [
      rehypeAddMDXExports,
      (tree) => ({
         sections: `[${getSections(tree).join()}]`,
      }),
   ],
]

