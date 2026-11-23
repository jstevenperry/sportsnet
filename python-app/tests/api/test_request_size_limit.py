import asyncio

from sportsnet.main import RequestSizeLimitMiddleware


def test_request_limit_counts_streamed_chunks_without_content_length() -> None:
    called = False
    sent: list[dict[str, object]] = []
    messages = [
        {"type": "http.request", "body": b"123", "more_body": True},
        {"type": "http.request", "body": b"456", "more_body": False},
    ]

    async def app(scope, receive, send) -> None:
        nonlocal called
        called = True

    async def receive():
        return messages.pop(0)

    async def send(message) -> None:
        sent.append(message)

    middleware = RequestSizeLimitMiddleware(app, max_request_body_bytes=5)
    asyncio.run(
        middleware(
            {"type": "http", "method": "POST", "path": "/", "headers": []},
            receive,
            send,
        )
    )

    assert called is False
    assert (
        next(item["status"] for item in sent if item["type"] == "http.response.start")
        == 413
    )
