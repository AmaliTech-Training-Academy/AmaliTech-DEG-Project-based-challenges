import asyncio
import time
from fastapi import FastAPI, Header, HTTPException
from fastapi.responses import JSONResponse
from typing import Optional

from models import PaymentRequest, PaymentResponse
from store import (
    _hash_body,
    get_key_lock,
    get_entry,
    create_entry,
    complete_entry,
    body_matches,
    wait_for_completion,
)
from audit import log, get_log, Outcome


app = FastAPI(
    title="Idempotency Gateway",
    description="A payment processing API that guarantees each request is processed exactly once.",
    version="1.0.0",
)

_key_timestamps: dict = {}
KEY_TTL_SECONDS = 86_400


def _build_response(amount: float, currency: str, idempotency_key: str) -> dict:
    return {
        "status": "success",
        "message": f"Charged {amount} {currency}",
        "idempotency_key": idempotency_key,
        "amount": amount,
        "currency": currency,
    }


@app.post(
    "/process-payment",
    status_code=201,
    response_model=PaymentResponse,
    responses={
        200: {"description": "Duplicate request — cached response returned"},
        400: {"description": "Missing Idempotency-Key header"},
        422: {"description": "Same key reused with a different request body"},
    },
    summary="Process a payment (idempotent)",
)
async def process_payment(
    payment: PaymentRequest,
    idempotency_key: Optional[str] = Header(default=None, alias="Idempotency-Key"),
):
    if not idempotency_key:
        log(Outcome.INVALID, 400)
        raise HTTPException(status_code=400, detail="Missing required header: Idempotency-Key")

    body_hash = _hash_body(payment.model_dump())
    key_lock = await get_key_lock(idempotency_key)
    should_process = False

    async with key_lock:
        entry = get_entry(idempotency_key)

        if entry is not None:
            if entry["status"] == "done" and body_matches(idempotency_key, body_hash):
                log(Outcome.DUPLICATE, 200, idempotency_key, payment.amount, payment.currency)
                return JSONResponse(content=entry["response"], status_code=200, headers={"X-Cache-Hit": "true"})

            if entry["status"] == "done" and not body_matches(idempotency_key, body_hash):
                log(Outcome.CONFLICT, 422, idempotency_key, payment.amount, payment.currency)
                raise HTTPException(status_code=422, detail="Idempotency key already used for a different request body.")

        else:
            _key_timestamps[idempotency_key] = time.time()
            create_entry(idempotency_key, body_hash)
            should_process = True

    if not should_process:
        entry = get_entry(idempotency_key)

        if entry is not None and entry["status"] == "done":
            if not body_matches(idempotency_key, body_hash):
                log(Outcome.CONFLICT, 422, idempotency_key, payment.amount, payment.currency)
                raise HTTPException(status_code=422, detail="Idempotency key already used for a different request body.")
            log(Outcome.DUPLICATE, 200, idempotency_key, payment.amount, payment.currency)
            return JSONResponse(content=entry["response"], status_code=200, headers={"X-Cache-Hit": "true"})

        if entry is not None and entry["status"] == "processing":
            await wait_for_completion(idempotency_key)
            finished = get_entry(idempotency_key)

            if not body_matches(idempotency_key, body_hash):
                log(Outcome.CONFLICT, 422, idempotency_key, payment.amount, payment.currency)
                raise HTTPException(status_code=422, detail="Idempotency key already used for a different request body.")

            log(Outcome.IN_FLIGHT, 200, idempotency_key, payment.amount, payment.currency)
            return JSONResponse(content=finished["response"], status_code=200, headers={"X-Cache-Hit": "true"})

        raise HTTPException(status_code=500, detail="Unexpected state: entry missing after lock.")

    await asyncio.sleep(2)

    response_data = _build_response(payment.amount, payment.currency, idempotency_key)
    complete_entry(idempotency_key, response_data, 201)
    log(Outcome.PROCESSED, 201, idempotency_key, payment.amount, payment.currency)
    return JSONResponse(content=response_data, status_code=201)


@app.get("/audit-log", summary="Full audit trail of every payment request", tags=["Operations"])
async def audit_log():
    return [entry.model_dump() for entry in get_log()]


@app.get("/store-stats", summary="Operator dashboard: key counts and expiry info", tags=["Operations"])
async def store_stats():
    now = time.time()
    total = len(_key_timestamps)
    expired = sum(1 for ts in _key_timestamps.values() if now - ts > KEY_TTL_SECONDS)
    return {
        "total_keys": total,
        "active_keys": total - expired,
        "expired_keys": expired,
        "ttl_seconds": KEY_TTL_SECONDS,
    }


@app.get("/health", tags=["Operations"], summary="Health check")
async def health():
    return {"status": "ok"}