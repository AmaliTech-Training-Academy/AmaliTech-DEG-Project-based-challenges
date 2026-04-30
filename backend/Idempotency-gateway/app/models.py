from pydantic import BaseModel, Field
from typing import Optional

class PaymentRequest(BaseModel):
    amount: float = Field(..., gt=0, description="Payment amount must be greater than 0")
    currency: str = Field(..., min_length=3, max_length=3, description="3-letter currency code like GHS, USD")
    
    class Config:
        json_schema_extra = {
            "example": {
                "amount": 100.00,
                "currency": "GHS"
            }
        }

class PaymentResponse(BaseModel):
    status: str
    message: str
    transaction_id: str
    amount: float
    currency: str