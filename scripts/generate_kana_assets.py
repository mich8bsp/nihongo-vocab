#!/usr/bin/env python3
"""One-off script: generate the hand-authored kana.json asset (hiragana +
katakana, seion + dakuten + handakuten - no youon combinations for v1).
Each row lists (hiragana, katakana, romaji-or-list-of-acceptable-romaji).
"""
import json
import pathlib

ROWS: list[tuple[str, str, str | list[str]]] = [
    # seion
    ("あ", "ア", "a"), ("い", "イ", "i"), ("う", "ウ", "u"), ("え", "エ", "e"), ("お", "オ", "o"),
    ("か", "カ", "ka"), ("き", "キ", "ki"), ("く", "ク", "ku"), ("け", "ケ", "ke"), ("こ", "コ", "ko"),
    ("さ", "サ", "sa"), ("し", "シ", "shi"), ("す", "ス", "su"), ("せ", "セ", "se"), ("そ", "ソ", "so"),
    ("た", "タ", "ta"), ("ち", "チ", "chi"), ("つ", "ツ", "tsu"), ("て", "テ", "te"), ("と", "ト", "to"),
    ("な", "ナ", "na"), ("に", "ニ", "ni"), ("ぬ", "ヌ", "nu"), ("ね", "ネ", "ne"), ("の", "ノ", "no"),
    ("は", "ハ", "ha"), ("ひ", "ヒ", "hi"), ("ふ", "フ", "fu"), ("へ", "ヘ", "he"), ("ほ", "ホ", "ho"),
    ("ま", "マ", "ma"), ("み", "ミ", "mi"), ("む", "ム", "mu"), ("め", "メ", "me"), ("も", "モ", "mo"),
    ("や", "ヤ", "ya"), ("ゆ", "ユ", "yu"), ("よ", "ヨ", "yo"),
    ("ら", "ラ", "ra"), ("り", "リ", "ri"), ("る", "ル", "ru"), ("れ", "レ", "re"), ("ろ", "ロ", "ro"),
    ("わ", "ワ", "wa"), ("を", "ヲ", "wo"),
    ("ん", "ン", "n"),
    # dakuten
    ("が", "ガ", "ga"), ("ぎ", "ギ", "gi"), ("ぐ", "グ", "gu"), ("げ", "ゲ", "ge"), ("ご", "ゴ", "go"),
    ("ざ", "ザ", "za"), ("じ", "ジ", "ji"), ("ず", "ズ", "zu"), ("ぜ", "ゼ", "ze"), ("ぞ", "ゾ", "zo"),
    ("だ", "ダ", "da"), ("ぢ", "ヂ", ["ji", "di"]), ("づ", "ヅ", ["zu", "du"]), ("で", "デ", "de"), ("ど", "ド", "do"),
    ("ば", "バ", "ba"), ("び", "ビ", "bi"), ("ぶ", "ブ", "bu"), ("べ", "ベ", "be"), ("ぼ", "ボ", "bo"),
    # handakuten
    ("ぱ", "パ", "pa"), ("ぴ", "ピ", "pi"), ("ぷ", "プ", "pu"), ("ぺ", "ペ", "pe"), ("ぽ", "ポ", "po"),
]

OUT_PATH = pathlib.Path(__file__).parent.parent / "app/src/main/assets/vocab/kana.json"


def main() -> None:
    entries = []
    for hira, kata, romaji in ROWS:
        meanings = romaji if isinstance(romaji, list) else [romaji]
        entries.append({"text": hira, "meanings": meanings})
        entries.append({"text": kata, "meanings": meanings})

    assert len(entries) == len(ROWS) * 2
    assert len(set(e["text"] for e in entries)) == len(entries), "duplicate kana text"

    OUT_PATH.write_text(json.dumps(entries, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"kana: {len(entries)} entries -> {OUT_PATH}")


if __name__ == "__main__":
    main()
