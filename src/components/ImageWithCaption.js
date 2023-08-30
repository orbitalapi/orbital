import clsx from 'clsx';
import Image from 'next/future/image';

export function ImageWithCaption({ caption, wide, src, voyagerLink, addLightBackground }) {

  return (
    <div className={clsx(
      "relative my-[2em] first:mt-0 last:mb-0 rounded-lg rounded-lg w-full mx-auto",
      { "bg-slate-800 dark:bg-transparent": addLightBackground },
    )}>
      <div className="p-[1rem]">
        <img src={src} />
        {/*<Image src={src}*/}
        {/*  alt={caption}*/}
        {/*     fill={true}*/}
        {/*       style={{*/}
        {/*         objectFit: 'contain',*/}
        {/*       }}*/}
        {/*  sizes="(max-width: 1280px) 100vw,*/}
        {/*            65vw"*/}
        {/*  className="rounded-md"*/}
        {/*/>*/}

        <div className="text-center text-slate-500 mt-[1rem]">{caption}</div>
        {voyagerLink !== undefined &&
          <div className="text-center"><a className="text-slate-400 text-sm" target='_blank'
            href={`https://voyager.orbitalhq.com/s/${voyagerLink}`}>Edit this diagram on
            voyager.vyne.co</a></div>}
      </div>
    </div>
  );
}
