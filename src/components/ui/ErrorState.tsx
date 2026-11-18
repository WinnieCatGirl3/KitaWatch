import { WifiOff, RotateCcw } from 'lucide-react';
import Button from './Button';

interface Props {
  title?: string;
  message?: string;
  /** Optional per-step failure reasons (e.g. one per provider). */
  details?: string[];
  onRetry?: () => void;
}

export default function ErrorState({
  title = 'Something went wrong',
  message,
  details,
  onRetry,
}: Props) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-2xl bg-ink-900 p-6 text-center ring-1 ring-white/5 sm:p-10">
      <WifiOff className="h-9 w-9 text-zinc-600 sm:h-10 sm:w-10" />
      <p className="font-semibold text-zinc-200">{title}</p>
      {message && <p className="max-w-sm text-sm text-zinc-500">{message}</p>}
      {details && details.length > 0 && (
        <div className="max-h-40 w-full max-w-md overflow-y-auto rounded-lg bg-ink-800/80 p-3 text-left ring-1 ring-white/5">
          {details.map((d, i) => (
            <p key={i} className="font-mono text-[11px] leading-relaxed text-zinc-500">
              {d}
            </p>
          ))}
        </div>
      )}
      {onRetry && (
        <Button variant="outline" size="md" onClick={onRetry}>
          <RotateCcw className="h-4 w-4" /> Retry
        </Button>
      )}
    </div>
  );
}