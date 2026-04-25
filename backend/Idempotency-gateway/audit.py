from datetime import datetime, timezone
from typing import List, Optional
from models import Outcome, AuditEntry

_audit_log: List[AuditEntry] = []
_counter: int = 0


def log(
    outcome: Outcome,
    status_code: int,
    idempotency_key: Optional[str] = None,
    amount: Optional[float] = None,
    currency: Optional[str] = None,
) -> None:
    global _counter
    _counter += 1

    entry = AuditEntry(
        id=_counter,
        timestamp=datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        idempotency_key=idempotency_key,
        amount=amount,
        currency=currency,
        outcome=outcome,
        status_code=status_code,
    )

    _audit_log.append(entry)


def get_log() -> List[AuditEntry]:
    return list(_audit_log)