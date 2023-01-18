import fs from 'fs'
import ReactDOMServer from 'react-dom/server'
import {MDXProvider} from '@mdx-js/react'
import {Feed} from 'feed'
import {getAllPosts} from '@/utils/getAllPosts'
import {mdxComponents} from '@/utils/mdxComponents'

export default function buildRss() {
  const baseUrl = 'https://orbitalhq.com'
  const blogUrl = `${baseUrl}/blog`

  const feed = new Feed({
    title: 'Orbital Blog',
    description: 'Everything Orbital.  From our keyboards straight to your eyeballs.',
    id: blogUrl,
    link: blogUrl,
    language: 'en',
    image: `${baseUrl}/favicons/favicon-32x32.png?v=3`,
    favicon: `${baseUrl}/favicons/favicon.ico?v=3`,
    copyright: `All rights reserved ${new Date().getFullYear()}, Orbital API Ltd`,
    feedLinks: {
      rss: `${baseUrl}/feeds/feed.xml`,
      json: `${baseUrl}/feeds/feed.json`,
      atom: `${baseUrl}/feeds/atom.xml`,
    },
    author: {
      name: 'Marty Pitt',
      link: 'https://twitter.com/@marty_pitt',
    },
  })

  getAllPosts().forEach(({slug, module: {meta, default: Content}}) => {
    const mdx = (
      <MDXProvider components={mdxComponents}>
        <Content/>
      </MDXProvider>
    )
    const html = ReactDOMServer.renderToStaticMarkup(mdx)
    const postText = `<p><em>(The post <a href="${blogUrl}/${slug}">${meta.title}</a> appeared first on <a href="${blogUrl}">Orbital Blog</a>.)</em></p>`

    let image = meta.ogImage ?? meta.image
    image = image
      ? `${baseUrl}${image.default?.src ?? image.src ?? image}`
      : `${baseUrl}/api/og?path=/blog/${slug}`

    feed.addItem({
      title: meta.title,
      id: meta.title,
      link: `${blogUrl}/${slug}`,
      description: meta.description,
      content: html + postText,
      author: meta.authors.map(({name, twitter}) => ({
        name,
        link: `https://twitter.com/${twitter}`,
      })),
      date: new Date(meta.date),
      image,
      ...(meta.discussion
        ? {
          comments: meta.discussion,
          extensions: [
            {
              name: '_comments',
              objects: {
                about: 'Link to discussion forum',
                comments: meta.discussion,
              },
            },
          ],
        }
        : {}),
    })
  })

  fs.mkdirSync('./public/feeds', {recursive: true})
  fs.writeFileSync('./public/feeds/feed.xml', feed.rss2())
  fs.writeFileSync('./public/feeds/atom.xml', feed.atom1())
  fs.writeFileSync('./public/feeds/feed.json', feed.json1())
}
