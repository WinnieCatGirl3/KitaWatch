"""Starts the bundled local services on loopback, mirroring the desktop sidecars.

Desktop builds spawn `uvicorn api:app` and `uvicorn server:app` as separate child
processes (see src-tauri/src/api_sidecar.rs: Kuhi API on 8000, proxy on 8001).
Android embeds Python in the app process (Chaquopy) instead, so the same servers
run in daemon threads here.

Everything is imported lazily inside the threads: the heavy modules (uvicorn,
httpx, curl_cffi) take seconds to import on a phone, and `start()` is called from
the app's startup path where blocking would delay the first frame.
"""

import threading


def _kuhi_app():
    import api  # noqa: F401  -> api.py pulls in src.main:app

    return api.app


def _proxy_app():
    from kitawatch_proxy import server

    return server.app


_SERVICES = (
    (_kuhi_app, 8000, "kuhi-api"),
    (_proxy_app, 8001, "proxy"),
)


def _run_service(get_app, port: int, label: str) -> None:
    """Import one bundled app and serve it; blocking, runs in its own thread."""
    try:
        import uvicorn

        # No reload=True (that needs multiprocessing, which Android lacks) and
        # bind to loopback only, like the desktop sidecars do.
        uvicorn.run(get_app(), host="127.0.0.1", port=port, log_level="warning")
    except BaseException as exc:  # noqa: BLE001 - surface in Logcat
        # stdout/stderr go to Logcat (tag: python.stdout / python.stderr)
        print(f"[kitawatch] {label} failed to start: {exc!r}")
        raise


def start() -> None:
    """Run the bundled services in daemon threads; idempotent, returns immediately."""
    if getattr(start, "_running", False):
        return
    start._running = True

    for get_app, port, label in _SERVICES:
        threading.Thread(
            target=_run_service,
            args=(get_app, port, label),
            daemon=True,
            name=label,
        ).start()
