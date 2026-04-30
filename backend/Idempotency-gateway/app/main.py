from fastapi import FastAPI, Header, HTTPException
from fastapi.responses import JSONResponse
from typing import Optional
import uuid
import asyncio
import hashlib
import json
from .models import PaymentRequest, PaymentResponse, ErrorResponse
from .storage.memory_storage import MemoryStorage

app = FastAPI(title="Idempotency Gateway", description="Pay Once Protocol")

# Initialize storage
storage = MemoryStorage()

def get_request_hash(payment: PaymentRequest) -> str:
    """Create a hash of the request body for validation"""
    request_data = {
        "amount": payment.amount,
        "currency": payment.currency
    }
    # Sort keys to ensure consistent hash
    request_string = json.dumps(request_data, sort_keys=True)
    return hashlib.sha256(request_string.encode()).hexdigest()

def simulate_payment_processing(amount: float, currency: str) -> dict:
    """Simulate payment processing with 2-second delay"""
    import time
    time.sleep(2)  # Simulate processing time
    
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
            # Different request body with same key - REJECT!
            raise HTTPException(
                status_code=409,
                detail={
                    "error": "Conflict",
                    "message": "Idempotency key already used for a different request body",
                    "idempotency_key": idempotency_key
                }
            )
        
        # Same key and same body - return cached response with cache header
        response = PaymentResponse(**cached_response)
        # Return as JSONResponse to add custom headers
        return JSONResponse(
            content=response.dict(),
            status_code=200,
            headers={"X-Cache-Hit": "true"}
        )
    
    # Step 5: Get lock for this key (handles concurrent requests)
    lock = await storage.get_lock(idempotency_key)
    
    # Step 6: Acquire lock (wait if another request is processing)
    async with lock:
        # Double-check cache (in case another request just completed)
        cached_response = await storage.get(idempotency_key)
        stored_hash = await storage.get_request_hash(idempotency_key)
        
        if cached_response and stored_hash:
            # Validate again
            if stored_hash != current_request_hash:
                raise HTTPException(
                    status_code=409,
                    detail={
                        "error": "Conflict",
                        "message": "Idempotency key already used for a different request body",
                        "idempotency_key": idempotency_key
                    }
                )
            response = PaymentResponse(**cached_response)
            return JSONResponse(
                content=response.dict(),
                status_code=200,
                headers={"X-Cache-Hit": "true"}
            )
        
        # Step 7: Process the payment (with 2-second delay)
        result = simulate_payment_processing(payment.amount, payment.currency)
        
        # Step 8: Store the result with request hash
        await storage.set(idempotency_key, result, current_request_hash)
        
        # Step 9: Return the result (this is a fresh response, not from cache)
        response = PaymentResponse(**result)
        return JSONResponse(
            content=response.dict(),
            status_code=200,
            headers={"X-Cache-Hit": "false"}
        )