import pytest


def pytest_collection_modifyitems(items: list[pytest.Item]) -> None:
    directory_markers = {
        "unit": "unit",
        "repository": "repository",
        "keras": "keras",
        "api": "api",
        "e2e": "e2e",
        "integration": "integration",
    }
    for item in items:
        for directory, marker in directory_markers.items():
            if directory in item.path.parts:
                item.add_marker(getattr(pytest.mark, marker))
                break
