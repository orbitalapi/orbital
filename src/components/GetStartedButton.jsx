import NextLink from 'next/link';


export default function GetStartedButton() {

    return (
        <NextLink href='/docs'>
            <a
                className='bg-citrus min-w-[170px] hover:bg-citrus-300 focus:outline-none focus:ring-2 focus:ring-slate-400 focus:ring-offset-2 focus:ring-offset-slate-50 text-midnight-blue font-bold h-12 px-6 rounded-full sm:flex-1 w-48 flex items-center justify-center dark:bg-citrus dark:highlight-white/20 dark:hover:bg-citrus-300 uppercase tracking-wider'>
                Get started
            </a>
        </NextLink>
    )
}