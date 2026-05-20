import json
from typing import Any

from app.main import app


async def request_app(
    method: str,
    path: str,
    json_body: dict[str, Any] | None = None,
    headers: dict[str, str] | None = None,
) -> tuple[int, dict[str, Any]]:
    body_bytes = b""
    request_headers: list[tuple[bytes, bytes]] = []

    if json_body is not None:
        body_bytes = json.dumps(json_body).encode("utf-8")
        request_headers = [
            (b"content-type", b"application/json"),
            (b"content-length", str(len(body_bytes)).encode("ascii")),
        ]

    if headers is not None:
        request_headers.extend(
            (key.lower().encode("ascii"), value.encode("utf-8"))
            for key, value in headers.items()
        )

    messages: list[dict[str, Any]] = []

    async def receive() -> dict[str, Any]:
        return {
            "type": "http.request",
            "body": body_bytes,
            "more_body": False,
        }

    async def send(message: dict[str, Any]) -> None:
        messages.append(message)

    scope = {
        "type": "http",
        "asgi": {"version": "3.0", "spec_version": "2.3"},
        "http_version": "1.1",
        "method": method,
        "scheme": "http",
        "path": path,
        "raw_path": path.encode("ascii"),
        "query_string": b"",
        "headers": request_headers,
        "client": ("testclient", 50000),
        "server": ("testserver", 80),
        "root_path": "",
    }

    await app(scope, receive, send)

    status = next(
        message["status"]
        for message in messages
        if message["type"] == "http.response.start"
    )
    response_body = b"".join(
        message.get("body", b"")
        for message in messages
        if message["type"] == "http.response.body"
    )
    return status, json.loads(response_body)
