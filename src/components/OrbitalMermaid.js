import { useEffect } from 'react';
import { useTheme } from '@/components/ThemeToggle';
import mermaid from 'mermaid';
import { Mermaid } from 'mdx-mermaid/Mermaid';
import clsx from 'clsx';

/**
 * This is a small wrapper around Mermaid which
 * correctly detects if we're using dark mode, and sets the theme.
 *
 * While mdx-mermaid is supposed to support this, it doesn't appear to work.
 *
 * Currently, re-initializing the theme doesn't work, so switching between
 * dark and light within the same session leaves the chart unrendered.
 */
export default function OrbitalMermaid({ chart, wide }) {
  const extraWideImageBreakoutContainer = wide ? {
    position: 'relative',
    right: '50%',
    width: '60vw'
  } : {};

  // Don't render server-side, as it screws up
  // the measuring of the diagrams.
  const isSSR = () => typeof window === 'undefined';

  const [theme, setTheme] = useTheme();
  useEffect(() => {
    if (theme === null) {
      return;
    }
    const mermaidTheme = theme === 'dark' ? 'dark' : 'default';
    mermaid.initialize({
      theme: mermaidTheme
    });
  }, [theme]);

  return (<div style={extraWideImageBreakoutContainer}>
    <div className={clsx(
      'relative my-[2em] first:mt-0 last:mb-0 rounded-lg overflow-hidden rounded-lg w-full'
    )}>
      {!isSSR() && <Mermaid chart={chart} />}

    </div>
  </div>);
}
