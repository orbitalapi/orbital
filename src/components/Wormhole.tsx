import clsx from 'clsx';
import Image from 'next/future/image';
import WormholeImg from '@/img/wormhole-citrus-transparent.png'


export default function Wormhole(props) {
    return (
        <div className={clsx(
            props.className,
            'sm:hidden md:w-[500px] lg:w-[600px] md:h-[500px] lg:h-[600px] absolute top-0 right-0 block rotate-[130deg] md:opacity-25 lg:opacity-50 pointer-events-none z-0',
        )}>
            <Image src={WormholeImg}
                fill
                priority
                alt="Orbital wormhole logo"
                sizes="(max-width: 767px) 0vw,
                    (max-width: 1280px) 50vw,
                    33vw"
            />
        </div>
    );
}
