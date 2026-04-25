import asyncio
import hashlib
import json
from typing import Optional, Dict, Any


_store: Dict[str, Dict[str, Any]] = {}
_locks: Dict[str, asyncio.Lock] = {}
_global_lock = asyncio.Lock()


def _hash_body(body: dict) -> str:
    serialized = json.dumps(body, sort_keys=True)
    return hashlib.sha256(serialized.encode()).hexdigest()


async def get_key_lock(key: str) -> asyncio.Lock:
    async with _global_lock:
        if key not in _locks:
            _locks[key] = asyncio.Lock()
        return _locks[key]


def get_entry(key: str) -> Optional[Dict[str, Any]]:
    return _store.get(key)


def create_entry(key: str, body_hash: str) -> None:
    _store[key] = {
        "status": "processing",
        "body_hash": body_hash,
        "response": None,
        "status_code": None,
        "event": asyncio.Event(),
    }


def complete_entry(key: str, response: dict, status_code: int) -> None:
    entry = _store[key]
    entry["status"] = "done"
    entry["response"] = response
    entry["status_code"] = status_code
    entry["event"].set()


def body_matches(key: str, body_hash: str) -> bool:
    return _store[key]["body_hash"] == body_hash


async def wait_for_completion(key: str) -> None:
    event: asyncio.Event = _store[key]["event"]
    await event.wait()