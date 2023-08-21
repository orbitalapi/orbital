import NextLink from "next/link";
import {ReactElement, ReactNode} from "react";

type LinkButtonProps = {
  label: string;
  link: string;
  icon?: ReactElement
  styles?: string;
}
export const LinkButton = ({ label, link, icon, styles } : LinkButtonProps) => {
  return (
    <NextLink href={link}>
      <a
        className={`${styles || ''} bg-midnight-blue hover:bg-slate-300/20 color-white text-white font-bold h-12 px-6
                rounded-lg border-2 border-white  flex sm:flex-1 items-center justify-center`}>

        <div className='w-4 mr-5 '>
          {icon}
        </div>

        {label}

      </a>
    </NextLink>
  )
}
