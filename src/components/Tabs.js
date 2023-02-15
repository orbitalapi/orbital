export function Tabs({tabs, selected, onChange, className, iconClassName}) {
  return (
    <div className="flex overflow-auto sm:mx-0 w-full">
      <ul
        className="flex-none inline-grid gap-x-2 px-4 sm:px-0 gap-y-8 xl:gap-x-6 w-full"
        style={{gridTemplateColumns: `repeat(auto-fit, minmax(7rem, 1fr))`}}
      >
        {Object.entries(tabs).map(([name, icon]) => (
          <li key={name}>
            <button
              type="button"
              onClick={() => onChange(name)}
              className={`group text-sm font-semibold w-full flex flex-col items-center ${
                selected === name ? className : ''
              }`}
            >
              <svg
                width="48"
                height="48"
                fill="none"
                aria-hidden="true"
                className={`mb-6 ${
                  selected === name
                    ? iconClassName
                    : 'text-slate-300 group-hover:text-slate-400 dark:text-slate-600 dark:group-hover:text-slate-500'
                }`}
              >
                {icon(selected === name)}
              </svg>
              {name}
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}
