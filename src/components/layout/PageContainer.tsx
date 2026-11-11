import { forwardRef } from 'react';
import { motion } from 'framer-motion';
import type { ReactNode } from 'react';

interface Props {
  children: ReactNode;
  className?: string;
}

/** Scrollable page body with a soft entrance animation.
 *  Ref is forwarded to the <main> so virtualized grids can use it
 *  as their scroll element. */
const PageContainer = forwardRef<HTMLElement, Props>(function PageContainer(
  { children, className = '' },
  ref,
) {
  return (
    // overflow-x-hidden: one over-wide child (measured: the torrent search
    // row) turned the whole page into a horizontal scroller on the phone,
    // shifting every card 13px off-screen to the left.
    <motion.main
      ref={ref}
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -8 }}
      transition={{ duration: 0.25, ease: 'easeOut' }}
      className={`relative min-h-0 min-w-0 flex-1 space-y-8 overflow-y-auto overflow-x-hidden p-4 sm:p-6 ${className}`}
    >
      {children}
    </motion.main>
  );
});

export default PageContainer;
