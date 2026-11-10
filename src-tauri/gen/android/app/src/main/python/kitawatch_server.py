"""Starts the bundled Kuhi API on loopback, mirroring the desktop sidecar.

Desktop builds spawn `uvicorn api:app` as a separate child process (see
src-tauri/src/api_sidecar.rs). Android embeds Python in the app process
(Chaquopy) instead, so the same server runs in a daemon thread here.

Everything is imported lazily inside the thread: the heavy modules (uvicorn,
httpx, curl_cffi) take seconds to import on a phone, and `start()` is called
from the app's startup path where blocking would delay the first frame.
"""

import threading


def start(port: int = 8000, host: str = "127.0.0.1") -> None:
    """Run uvicorn in a daemon thread; idempotent, returns immediately."""
    if getattr(start, "_running", False):
        return
    start._running = True

    def _run() -> None:
        try:
            import uvicorn

            import api  # noqa: F401  -> api.py pulls in src.main:app

            # No reload=True (that needs multiprocessing, which Android lacks)
            # and bind to loopback only, like the desktop sidecar does.
            uvicorn.run(api.app, host=host, port=port, log_level="warning")
        except BaseException as exc:  # noqa: BLE001 - surface in Logcat
            # stdout/stderr go to Logcat (tag: python.stdout / python.stderr)
            print(f"[kitawatch] kuhi-api failed to start: {exc!r}")
            raise

    threading.Thread(target=_run, daemon=True, name="kuhi-api").start()
