import pydantic
from pydantic import BaseModel, Field
from typing import Optional
from enum import Enum


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


class ErrorResponse(pydantic.BaseModel):
    error: str
    detail: Optional[str] = None


class Outcome(str, Enum):
    PROCESSED = "PROCESSED"
    DUPLICATE = "DUPLICATE"
    CONFLICT  = "CONFLICT"
    IN_FLIGHT = "IN_FLIGHT"
    INVALID   = "INVALID"


class AuditEntry(BaseModel):
    id:              int
    timestamp:       str
    idempotency_key: Optional[str]
    amount:          Optional[float]
    currency:        Optional[str]
    outcome:         Outcome
    status_code:     int