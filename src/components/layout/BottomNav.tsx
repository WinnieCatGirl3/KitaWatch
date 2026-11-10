import { NavLink, useLocation } from 'react-router-dom';
import { navItems } from '@/components/layout/Sidebar';

/**
 * Mobile navigation. The sidebar is hidden below the `md` breakpoint, so on
 * phones/tablets the same destinations live in a bottom bar instead — without
 * it the app would have no navigation on Android at all.
 *
 * Shares `navItems` (icons + active-match logic) with the desktop sidebar so
 * the two can never drift apart.
 */
export default function BottomNav() {
  const { pathname, search } = useLocation();

  return (
    <nav
      aria-label="Primary"
      className="flex shrink-0 items-stretch border-t border-white/10 bg-ink-900 md:hidden"
    >
      {navItems.map(({ to, label, icon: Icon, match }) => {
        const active = match(pathname, search);
        return (
          <NavLink
            key={label}
            to={to}
            aria-current={active ? 'page' : undefined}
            className={`flex min-w-0 flex-1 flex-col items-center justify-center gap-0.5 px-1 py-2 text-[10px] font-medium transition ${
              active
                ? 'text-accent-300'
                : 'text-zinc-400 active:text-zinc-100'
            }`}
          >
            <Icon className="h-5 w-5" />
            <span className="w-full truncate text-center leading-tight">
              {label}
            </span>
          </NavLink>
        );
      })}
    </nav>
  );
}
