import { Author } from '@/pages/blog';
import Image from 'next/future/image';

export function AuthorAvatar(author: Author) {
  return (<div
    key={author.twitter}
    className='flex items-center font-medium whitespace-nowrap mt-6'
  >
    { author.avatar !== undefined &&
      <Image
        src={author.avatar}
        alt={`Avatar for ${author.name}`}
        width={36}
        height={36}
        className='mr-3 rounded-full bg-slate-50 dark:bg-slate-800'
        decoding='async'
      />
    }
    <div className='text-sm leading-4'>
      <div className='text-slate-900 dark:text-slate-200'>{author.name}</div>
      <div className='mt-1'>
        <a
          href={`https://twitter.com/${author.twitter}`}
          className='text-sky-500 hover:text-sky-600 dark:text-sky-400'
        >
          @{author.twitter}
        </a>
      </div>
    </div>
  </div>);
}
