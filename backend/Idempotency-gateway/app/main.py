from fastapi import FastAPI, Header, HTTPException
from typing import Optional
from .models import PaymentRequest, PaymentResponse

app = FastAPI(title="Idempotency Gateway", description="Pay Once Protocol")

@app.get("/")
def root():
    return {"message": "Idempotency Gateway Running"}

@app.post("/process-payment", response_model=PaymentResponse)
async def process_payment(
    payment: PaymentRequest,
    idempotency_key: Optional[str] = Header(None, alias="Idempotency-Key")
):
    # Validate idempotency key is present
    if not idempotency_key:
        raise HTTPException(
            status_code=400, 
            detail="Idempotency-Key header is required"
        )
    
    # For now, just return a mock response
    # We'll add the actual processing logic later
    return PaymentResponse(
        status="success",
        message=f"Charged {payment.amount} {payment.currency}",
        transaction_id="mock-txn-123",
        amount=payment.amount,
        currency=payment.currency
    )