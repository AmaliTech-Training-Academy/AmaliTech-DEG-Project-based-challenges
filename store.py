import asyncio
import hashlib
import json
from typing import Optional, Dict, Any


# ---------------------------------------------------------------------------
# The entire storage layer is a plain Python dictionary.
# No Redis, no database — just an in-memory dict that lives for the lifetime
# of the server process.
#
# Structure of each entry:
# {
#   "status":       "processing" | "done",
#   "body_hash":    str,          # SHA-256 of the original request body
#   "response":     dict | None,  # saved response once processing is done
#   "status_code":  int | None,
#   "event":        asyncio.Event # lets waiting requests block until done
# }
# ---------------------------------------------------------------------------

_store: Dict[str, Dict[str, Any]] = {}

# One asyncio.Lock per idempotency key prevents race conditions.
# When two identical requests arrive at the same time, only the first one
# acquires the per-key lock and processes.  The second one waits.
_locks: Dict[str, asyncio.Lock] = {}

# A global lock used only to safely CREATE per-key locks without a race.
_global_lock = asyncio.Lock()


def _hash_body(body: dict) -> str:
    """Return a stable SHA-256 fingerprint of the request body."""
    serialized = json.dumps(body, sort_keys=True)
    return hashlib.sha256(serialized.encode()).hexdigest()


async def get_key_lock(key: str) -> asyncio.Lock:
    """Return (creating if needed) the asyncio.Lock dedicated to *key*."""
    async with _global_lock:
        if key not in _locks:
            _locks[key] = asyncio.Lock()
        return _locks[key]


def get_entry(key: str) -> Optional[Dict[str, Any]]:
    """Return the stored entry for *key*, or None if it doesn't exist."""
    return _store.get(key)


def create_entry(key: str, body_hash: str) -> None:
    """
    Create a brand-new entry in status 'processing'.
    An asyncio.Event is stored so duplicate in-flight requests can wait.
    """
    _store[key] = {
        "status": "processing",
        "body_hash": body_hash,
        "response": None,
        "status_code": None,
        "event": asyncio.Event(),   # will be set() once processing finishes
    }


def complete_entry(key: str, response: dict, status_code: int) -> None:
    """
    Mark an entry as 'done', save the response, and unblock any waiters.
    """
    entry = _store[key]
    entry["status"] = "done"
    entry["response"] = response
    entry["status_code"] = status_code
    entry["event"].set()           # wake up any requests that were waiting


def body_matches(key: str, body_hash: str) -> bool:
    """Return True if the stored body hash matches the incoming body hash."""
    return _store[key]["body_hash"] == body_hash


async def wait_for_completion(key: str) -> None:
    """
    Block the current coroutine until the entry for *key* is marked 'done'.
    Used by the race-condition (in-flight) path.
    """
    event: asyncio.Event = _store[key]["event"]
    await event.wait()