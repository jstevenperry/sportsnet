from pathlib import Path

import pytest

from sportsnet.domain.errors import ValidationError
from sportsnet.sports.ncaabb.csv_repository import canonicalize_team, load_aliases


def test_source_team_can_have_multiple_aliases(tmp_path: Path) -> None:
    path = tmp_path / "aliases.csv"
    path.write_text(
        "NAME IN SEASON DATA FILE,ALIAS\n"
        "Alabama State\n"
        "Alabama State,Alabama St\n"
        "Alabama State,Alabama State NCAA\n",
        encoding="utf-8",
    )

    aliases = load_aliases(path)

    assert canonicalize_team("Alabama State", aliases) == "Alabama State"
    assert canonicalize_team("Alabama St", aliases) == "Alabama State"
    assert canonicalize_team("Alabama State NCAA", aliases) == "Alabama State"


def test_shared_aliases_and_multiple_sources_resolve_independently_of_order(
    tmp_path: Path,
) -> None:
    path = tmp_path / "aliases.csv"
    path.write_text(
        "# comment before header\nNAME IN SEASON DATA FILE,ALIAS\n"
        "Alabama State NCAA,Alabama St\n"
        "Alabama State,Alabama St\n"
        "Alabama State\n"
        "Alabama State NCAA,Alabama State\n",
        encoding="utf-8",
    )
    aliases = load_aliases(path)
    assert canonicalize_team("Alabama State NCAA", aliases) == "Alabama State"
    assert canonicalize_team("Alabama St", aliases) == "Alabama State"
    assert "NAME IN SEASON DATA FILE" not in aliases
    path.write_text(
        "NAME IN SEASON DATA FILE,ALIAS\n"
        "Alabama State\n"
        "Alabama State NCAA,Alabama State\n"
        "Alabama State,Alabama St\n"
        "Alabama State NCAA,Alabama St\n",
        encoding="utf-8",
    )
    assert load_aliases(path) == aliases


def test_canonicalize_rejects_cycles_in_external_alias_mapping() -> None:
    with pytest.raises(ValidationError, match="cyclic"):
        canonicalize_team("Alpha", {"Alpha": "Beta", "Beta": "Alpha"})
