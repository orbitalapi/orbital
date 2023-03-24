import clsx from 'clsx';

export function RawImageWithCaption({ caption, wide, src, voyagerLink, addLightBackground }) {

  return (
    <div className='breakout-image'>
      <div className={clsx(
        "relative my-[2em] first:mt-0 last:mb-0 rounded-lg rounded-lg w-full lg:w-[65vw] mx-auto",
        { "bg-slate-800 dark:bg-transparent": addLightBackground },
      )}>
        <div className="p-[1rem]">
        <img src={src} decoding="async" alt={caption}/>

          <div className="text-center text-slate-500 mt-[1rem]">{caption}</div>
          {voyagerLink !== undefined &&
            <div className="text-center"><a className="text-slate-400 text-sm" target='_blank'
              href={`https://voyager.vyne.co/s/${voyagerLink}`}>Edit this diagram on
              voyager.vyne.co</a></div>}
        </div>
      </div>
    </div>
  );
}
