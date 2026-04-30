from fastapi import FastAPI, Header, HTTPException
from fastapi.responses import JSONResponse
from typing import Optional
import uuid
import asyncio
import hashlib
import json
from .models import PaymentRequest, PaymentResponse, ErrorResponse
from .storage.memory_storage import MemoryStorage
from .utils.logger import logger

app = FastAPI(title="Idempotency Gateway", description="Pay Once Protocol")

# Initialize storage
storage = MemoryStorage()

def get_request_hash(payment: PaymentRequest) -> str:
    """Create a hash of the request body for validation"""
    request_data = {
        "amount": payment.amount,
        "currency": payment.currency
    }
    request_string = json.dumps(request_data, sort_keys=True)
    return hashlib.sha256(request_string.encode()).hexdigest()

def simulate_payment_processing(amount: float, currency: str) -> dict:
    """Simulate payment processing with 2-second delay"""
    import time
    time.sleep(2)
    
    return {
        "status": "success",
        "message": f"Charged {amount} {currency}",
        "transaction_id": str(uuid.uuid4()),
        "amount": amount,
        "currency": currency
    }

@app.get("/")
def root():
    return {"message": "Idempotency Gateway Running"}

@app.get("/metrics")
def get_metrics():
    """Get idempotency gateway metrics and statistics"""
    stats = logger.get_stats()
    
    # Calculate cache hit ratio
    total_processed = stats["cache_hits"] + stats["cache_misses"]
    cache_hit_ratio = stats["cache_hits"] / total_processed if total_processed > 0 else 0
    
    return {
        "statistics": stats,
        "cache_hit_ratio": f"{cache_hit_ratio * 100:.2f}%",
        "status": "healthy",
        "uptime_seconds": 0  # In production, you'd track actual uptime
    }

@app.post("/process-payment", 
          response_model=PaymentResponse,
          responses={
              409: {"model": ErrorResponse, "description": "Idempotency key already used for different request"}
          })
async def process_payment(
    payment: PaymentRequest,
    idempotency_key: Optional[str] = Header(None, alias="Idempotency-Key")
):
    # Step 1: Validate idempotency key is present
    if not idempotency_key:
        raise HTTPException(
            status_code=400, 
            detail="Idempotency-Key header is required"
        )
    
    # Step 2: Calculate request hash
    current_request_hash = get_request_hash(payment)
    
    # Step 3: Check if we've seen this key before
    cached_response = await storage.get(idempotency_key)
    stored_hash = await storage.get_request_hash(idempotency_key)
    
    if cached_response and stored_hash:
        # Step 4: Validate request body matches
        if stored_hash != current_request_hash:
            # Log the conflict
            logger.log_conflict(idempotency_key, {"hash": stored_hash}, {"hash": current_request_hash})
            
            raise HTTPException(
                status_code=409,
                detail={
                    "error": "Conflict",
                    "message": "Idempotency key already used for a different request body",
                    "idempotency_key": idempotency_key
                }
            )
        
        # Cache hit - log and return
        logger.log_cache_hit(idempotency_key)
        logger.log_request(idempotency_key, payment.dict(), "hit")
        
        response = PaymentResponse(**cached_response)
        return JSONResponse(
            content=response.dict(),
            status_code=200,
            headers={"X-Cache-Hit": "true"}
        )
    
    # Step 5: Get lock for this key
    lock = await storage.get_lock(idempotency_key)
    
    # Step 6: Acquire lock
    async with lock:
        # Double-check cache
        cached_response = await storage.get(idempotency_key)
        stored_hash = await storage.get_request_hash(idempotency_key)
        
        if cached_response and stored_hash:
            if stored_hash != current_request_hash:
                logger.log_conflict(idempotency_key, {"hash": stored_hash}, {"hash": current_request_hash})
                raise HTTPException(
                    status_code=409,
                    detail={
                        "error": "Conflict",
                        "message": "Idempotency key already used for a different request body",
                        "idempotency_key": idempotency_key
                    }
                )
            
            logger.log_cache_hit(idempotency_key)
            logger.log_request(idempotency_key, payment.dict(), "hit")
            
            response = PaymentResponse(**cached_response)
            return JSONResponse(
                content=response.dict(),
                status_code=200,
                headers={"X-Cache-Hit": "true"}
            )
        
        # Cache miss - process payment
        logger.log_cache_miss(idempotency_key)
        logger.log_request(idempotency_key, payment.dict(), "miss")
        
        result = simulate_payment_processing(payment.amount, payment.currency)
        await storage.set(idempotency_key, result, current_request_hash)
        
        response = PaymentResponse(**result)
        return JSONResponse(
            content=response.dict(),
            status_code=200,
            headers={"X-Cache-Hit": "false"}
        )