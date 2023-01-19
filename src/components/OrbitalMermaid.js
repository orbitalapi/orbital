import {useEffect} from 'react'
import {useTheme} from "@/components/ThemeToggle";
import mermaid from 'mermaid';
import {Mermaid} from "mdx-mermaid/Mermaid";

/**
 * This is a small wrapper around Mermaid which
 * correctly detects if we're using dark mode, and sets the theme.
 *
 * While mdx-mermaid is supposed to support this, it doesn't appear to work.
 *
 * Currently, re-initializing the theme doesn't work, so switching between
 * dark and light within the same session leaves the chart unrendered.
 */
export default function OrbitalMermaid({chart}) {


  const [theme, setTheme] = useTheme()
  useEffect(() => {
    if (theme === null) {
      return;
    }
    const mermaidTheme = theme === 'dark' ? 'dark' : 'default';
    mermaid.initialize({
      theme: mermaidTheme
    })
  }, [theme])

  return (<Mermaid chart={chart}/>);
}
