import wormholeCitrus from '@/img/wormhole-citrus-transparent.png';
import clsx from 'clsx';


export default function Wormhole(props) {
    return (
        <img src={wormholeCitrus.src}
            className={clsx(
                props.className,
                'sm:hidden md:h-[400px] lg:h-[600px] md:opacity-25 rotate-[130deg] absolute top-0 right-0 block',
            )}
        />
    );
}