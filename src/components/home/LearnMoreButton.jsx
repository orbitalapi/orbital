import clsx from 'clsx';


export default function LearnMoreButton({ href, text }) {
        const Text = text;

        return (
        <button
            type='button'
            formTarget="https://docs.taxilang.org"
            className='mt-4 px-3.5 py-2 border border-transparent text-sm leading-4 font-sm rounded-full shadow-sm text-midnight-blue bg-citrus hover:bg-citrus-300 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-citrus-700'
        >
            <a href={href} target="_blank" className="inline-flex items-center">
                <Text></Text>

                <svg
                    className={clsx(
                        'overflow-visible ml-5'
                    )}
                    width='3'
                    height='6'
                    viewBox='0 0 3 6'
                    fill='none'
                    stroke='currentColor'
                    strokeWidth='2'
                    strokeLinecap='round'
                    strokeLinejoin='round'
                >
                    <path d={'M0 0L3 3L0 6'} />
                </svg>
            </a>
        </button>
    );
}