from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True)
class CycleError(Exception):
    cycles: list[list[str]] = field(default_factory=list)

    def __str__(self) -> str:
        return f"Dependency cycles detected: {self.cycles}"


@dataclass(frozen=True)
class InvalidInputError(Exception):
    message: str = ""

    def __str__(self) -> str:
        return self.message
