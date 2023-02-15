import clsx from 'clsx';

export function ImageWithCaption({caption, wide, src, voyagerLink, addLightBackground}) {
  const extraWideImageBreakoutContainer = wide ? {
    position: "relative",
    right: "50%",
    width: "60vw",
  } : {}
  return (
    <div style={extraWideImageBreakoutContainer}>
      <div className={clsx(
        "relative my-[2em] first:mt-0 last:mb-0 rounded-lg overflow-hidden rounded-lg w-full",
        {"bg-slate-800 dark:bg-transparent": addLightBackground},
      )}>
        <div className="p-[1rem]">
          <img src={src} decoding="async" alt={caption} className='object-fill w-full'/>
          <div className="text-center text-slate-500 mt-[1rem]">{caption}</div>
          {voyagerLink !== undefined &&
            <div className="text-center"><a className="text-slate-400 text-sm" target='_blank'
                                            href={`https://voyager.vyne.co/s/${voyagerLink}`}>Edit this diagram on
              voyager.vyne.co</a></div>}
        </div>
        {/* <div className="" /> */}
      </div>
    </div>
  );
}
