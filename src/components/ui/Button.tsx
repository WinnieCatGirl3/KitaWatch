import type { ButtonHTMLAttributes } from 'react';

type Variant = 'primary' | 'outline' | 'ghost';
type Size = 'sm' | 'md';

const variants: Record<Variant, string> = {
  primary:
    'bg-accent-600 text-white shadow-lg shadow-accent-600/25 hover:bg-accent-500',
  outline: 'bg-white/5 text-zinc-200 ring-1 ring-white/15 hover:bg-white/10',
  ghost: 'text-zinc-400 hover:bg-ink-800 hover:text-white',
};

const sizes: Record<Size, string> = {
  // py-2.5 (not py-1.5): "sm" buttons are the primary touch targets on phones
  // (Previous/Next, Retry, Load more) and measured only ~28px tall at 12px
  // text — below the ~36px minimum for comfortable tapping.
  sm: 'px-3 py-2.5 text-xs',
  md: 'px-4 py-2 text-sm',
};

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
}

export default function Button({
  variant = 'primary',
  size = 'md',
  className = '',
  children,
  ...rest
}: Props) {
  return (
    <button
      className={`inline-flex items-center justify-center gap-2 rounded-lg font-medium transition focus:outline-none focus-visible:ring-2 focus-visible:ring-accent-500 disabled:pointer-events-none disabled:opacity-50 ${variants[variant]} ${sizes[size]} ${className}`}
      {...rest}
    >
      {children}
    </button>
  );
}
