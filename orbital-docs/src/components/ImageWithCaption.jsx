import clsx from 'clsx';

export function ImageWithCaption({caption, wide, src, voyagerLink}) {
  const extraWideImageBreakoutContainer = wide ? {
    marginLeft: "-50vw",
    left: "50%",
    marginRight: "-50vw",
    maxWidth: "100vw",
    position: "relative",
    right: "50%",
    width: "100vw"
  } : {}
  return (
    <div style={extraWideImageBreakoutContainer}>
      <div className={clsx(
        "relative my-[2em] first:mt-0 last:mb-0 rounded-lg overflow-hidden rounded-lg ring-1 ring-inset ring-slate-900/10",
        {"mx-[15vw]": wide}
      )}>
        <div className="p-[1rem]">
          <img src={src} decoding="async" alt={caption}/>
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
