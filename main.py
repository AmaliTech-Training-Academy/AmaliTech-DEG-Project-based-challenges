import asyncio
from fastapi import FastAPI, Header, Request, HTTPException
from fastapi.responses import JSONResponse
from typing import Optional

from models import PaymentRequest, PaymentResponse, ErrorResponse
from store import (
    _hash_body,
    get_key_lock,
    get_entry,
    create_entry,
    complete_entry,
    body_matches,
    wait_for_completion,
)

# ---------------------------------------------------------------------------
# App setup
# ---------------------------------------------------------------------------

app = FastAPI(
    title="Idempotency Gateway",
    description="A payment processing API that guarantees each request is processed exactly once.",
    version="1.0.0",
)


# ---------------------------------------------------------------------------
# Developer's Choice extra feature: request TTL expiry (key expiry tracker)
# ---------------------------------------------------------------------------
# Real fintech APIs expire idempotency keys after 24 hours so the store
# doesn't grow forever.  We track the timestamp of every entry and expose
# a GET /store-stats endpoint so operators can monitor key counts.
# ---------------------------------------------------------------------------

import time
_key_timestamps: dict = {}   # key -> unix timestamp of first request


# ---------------------------------------------------------------------------
# Helper: build the payment response dict
# ---------------------------------------------------------------------------

def _build_response(amount: float, currency: str, idempotency_key: str) -> dict:
    return {
        "status": "success",
        "message": f"Charged {amount} {currency}",
        "idempotency_key": idempotency_key,
        "amount": amount,
        "currency": currency,
    }


# ---------------------------------------------------------------------------
# POST /process-payment
# ---------------------------------------------------------------------------

@app.post(
    "/process-payment",
    status_code=201,
    response_model=PaymentResponse,
    responses={
        200: {"description": "Duplicate request — cached response returned"},
        400: {"description": "Missing Idempotency-Key header"},
        409: {"description": "In-flight request completed — result returned"},
        422: {"description": "Same key reused with a different request body"},
    },
    summary="Process a payment (idempotent)",
)
async def process_payment(
    payment: PaymentRequest,
    idempotency_key: Optional[str] = Header(default=None, alias="Idempotency-Key"),
):
    # ------------------------------------------------------------------
    # 1. Validate: header must be present
    # ------------------------------------------------------------------
    if not idempotency_key:
        raise HTTPException(
            status_code=400,
            detail="Missing required header: Idempotency-Key",
        )

    # ------------------------------------------------------------------
    # 2. Fingerprint the incoming request body
    # ------------------------------------------------------------------
    body_hash = _hash_body(payment.model_dump())

    # ------------------------------------------------------------------
    # 3. Acquire the per-key lock to prevent race conditions.
    #    Only one coroutine at a time proceeds past this lock for a given key.
    # ------------------------------------------------------------------
    key_lock = await get_key_lock(idempotency_key)

    async with key_lock:
        entry = get_entry(idempotency_key)

        # ---- CASE A: Key seen before ----------------------------------
        if entry is not None:

            # A1: Same key, body still being processed (race condition)
            if entry["status"] == "processing":
                # Release the lock and wait for the first request to finish,
                # then fall through to return its result.
                # We intentionally release the lock here so the original
                # coroutine can complete and call complete_entry().
                pass   # we'll wait AFTER the lock block — see below

            # A2: Same key, done, body matches → cache hit
            elif entry["status"] == "done" and body_matches(idempotency_key, body_hash):
                response_data = entry["response"]
                headers = {"X-Cache-Hit": "true"}
                return JSONResponse(
                    content=response_data,
                    status_code=entry["status_code"],
                    headers=headers,
                )

            # A3: Same key, done, body DIFFERENT → fraud / error
            elif entry["status"] == "done" and not body_matches(idempotency_key, body_hash):
                raise HTTPException(
                    status_code=422,
                    detail="Idempotency key already used for a different request body.",
                )

        # ---- CASE B: Brand-new key → start processing ----------------
        else:
            _key_timestamps[idempotency_key] = time.time()
            create_entry(idempotency_key, body_hash)

    # ------------------------------------------------------------------
    # 4. If we're here because the entry was "processing" (race condition),
    #    wait outside the lock for the first request to finish.
    # ------------------------------------------------------------------
    entry = get_entry(idempotency_key)
    if entry is not None and entry["status"] == "processing" and entry["body_hash"] == body_hash:
        await wait_for_completion(idempotency_key)
        finished = get_entry(idempotency_key)
        return JSONResponse(
            content=finished["response"],
            status_code=finished["status_code"],
            headers={"X-Cache-Hit": "true"},
        )

    # ------------------------------------------------------------------
    # 5. Simulate payment processing (2-second delay as per spec)
    # ------------------------------------------------------------------
    await asyncio.sleep(2)

    # ------------------------------------------------------------------
    # 6. Build and persist the response
    # ------------------------------------------------------------------
    response_data = _build_response(payment.amount, payment.currency, idempotency_key)
    complete_entry(idempotency_key, response_data, 201)

    return JSONResponse(content=response_data, status_code=201)


# ---------------------------------------------------------------------------
# Developer's Choice: GET /store-stats
# Shows operators how many keys are stored and flags any expired ones.
# A real system would run a background task to evict expired keys.
# ---------------------------------------------------------------------------

KEY_TTL_SECONDS = 86_400   # 24 hours — standard in fintech (Stripe, etc.)


@app.get(
    "/store-stats",
    summary="Operator dashboard: key counts and expiry info",
    tags=["Operations"],
)
async def store_stats():
    now = time.time()
    total = len(_key_timestamps)
    expired = sum(
        1 for ts in _key_timestamps.values()
        if now - ts > KEY_TTL_SECONDS
    )
    return {
        "total_keys": total,
        "active_keys": total - expired,
        "expired_keys": expired,
        "ttl_seconds": KEY_TTL_SECONDS,
        "note": "Expired keys are flagged but not yet evicted in this demo. "
                "A production system would run a background cleanup task.",
    }


# ---------------------------------------------------------------------------
# Health check
# ---------------------------------------------------------------------------

@app.get("/health", tags=["Operations"], summary="Health check")
async def health():
    return {"status": "ok"}