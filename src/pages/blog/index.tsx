import { Widont } from '@/components/Widont';
import { getAllPostPreviews } from '@/utils/getAllPosts';
import Link from 'next/link';
import { formatDate } from '@/utils/formatDate';
import buildRss from '@/scripts/build-rss';
import { AuthorAvatar } from '@/components/AuthorAvatar';
import WormholeAqua from '@/img/wormhole-aqua-transparent.png';
import WormholeCitrus from '@/img/wormhole-citrus-transparent.png';
//@ts-ignore
import { ReactComponent as DataPatternLight } from '@/img/data-pattern-light.svg';

export interface Author {
  name: string;
  twitter: string;
  avatar: string;
}

interface Post {
  slug: string;
  filename: string;
  module: {
    meta: {
      title: string;
      description: string;
      date: string;
      imageUrl?: string;
      hidden: boolean;
      authors: Author[]
    }
  };
}

const posts: Post[] = getAllPostPreviews();

function DefaultPostImage(post: Post) {
  return (<div className='relative h-60 overflow-hidden'>

    <div className='pl-8 pr-4 py-8 flex'>
      <h3 className='text-slate-50 text-3xl z-10'>{post.module.meta.title}</h3>
      <img className='pointer-events-none top-0 right-10 w-16 h-16 ml-6' src={WormholeCitrus.src}></img>
    </div>

    <img className='pointer-events-none blur-2xl opacity-60 absolute bottom-0 left-0 w-40' src={WormholeAqua.src}></img>
    <DataPatternLight className='pointer-events-none absolute bottom-[-10px] h-[80px] w-auto'></DataPatternLight>


  </div>);
}

export default function Blog() {


  return (
    <main className='max-w-[52rem] mx-auto px-4 pb-28 sm:px-6 md:px-8 xl:px-12 lg:max-w-8xl'>
      <header className='py-16 sm:text-center'>
        <h1 className='mb-4 text-3xl sm:text-4xl tracking-tight text-slate-900 font-extrabold dark:text-slate-200'>
          Latest Updates
        </h1>
        <p className='text-lg text-slate-700 dark:text-slate-400'>
          <Widont>All the latest Orbital news, direct from the team. From our outputs to your inputs.</Widont>
        </p>
        {/*TODO : Get the hubspot form */}
        {/*<section className="mt-3 max-w-sm sm:mx-auto sm:px-4">*/}
        {/*  <h2 className="sr-only">Sign up for our newsletter</h2>*/}
        {/*  <NewsletterForm action="https://app.convertkit.com/forms/3181837/subscriptions"/>*/}
        {/*</section>*/}
      </header>

      <div
        className='mx-auto mt-16 grid max-w-2xl auto-rows-fr grid-cols-1 gap-8 sm:mt-6 lg:mx-0 lg:max-w-none lg:grid-cols-3'>
        {posts
          .filter((post) => post.module.meta.hidden !== true)
          .map((post) => (
            <article
              key={post.slug}
              className='relative isolate flex flex-col rounded-2xl bg-gray-900'
            >
              {post.module.meta.imageUrl ? (<img src={post.module.meta.imageUrl} alt=''
                                                 className='absolute inset-0 -z-10 h-full w-full object-cover' />) :
                (<DefaultPostImage {...post}></DefaultPostImage>)}

              <div className='px-8 py-8'>
                <h3 className='mt-3 text-lg font-semibold leading-6 text-white'>
                <Link href={`/blog/${post.slug}`}>
                  <a className='flex items-center text-sm text-slate-50 hover:text-sky-500 text-xl font-medium'>
                    <span className='relative'>                    {post.module.meta.title}                  </span>

                  </a>
                </Link>
              </h3>

              <div className='gap-y-1 overflow-hidden text-sm leading-6 text-gray-300'>
                <time
                  dateTime={post.module.meta.date}>{formatDate(post.module.meta.date, '{MMMM} {DD}, {YYYY}')}</time>
                <AuthorAvatar {...post.module.meta.authors[0]}></AuthorAvatar>
              </div>
            </div>
          </article>
        ))}
      </div>

    </main>
  );
}

Blog.layoutProps = {
  meta: {
    title: 'Blog',
    description: 'All the latest Orbital news, from our keyboards to your eyeballs.'
  }
};

export async function getStaticProps() {
  if (process.env.NODE_ENV === 'production') {
    buildRss();
  }

  return { props: {} };
}
