from pydantic import BaseModel, Field
from typing import Optional


class PaymentRequest(BaseModel):
    amount: float = Field(..., gt=0, description="Amount to charge")
    currency: str = Field(..., min_length=3, max_length=3, description="3-letter currency code e.g. GHS")

    class Config:
        json_schema_extra = {
            "example": {
                "amount": 100,
                "currency": "GHS"
            }
        }


class PaymentResponse(BaseModel):
    status: str
    message: str
    idempotency_key: str
    amount: float
    currency: str


class ErrorResponse(BaseModel):
    error: str
    detail: Optional[str] = None