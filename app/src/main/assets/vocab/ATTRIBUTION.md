Vocabulary data (`n5.json`–`n1.json`) is reshaped from
[elzup/jlpt-word-list](https://github.com/elzup/jlpt-word-list)
(`src/n5.csv`–`src/n1.csv`), MIT licensed:

```
MIT License
Copyright (c) 2020 Jamie Sinclair
Copyright (c) 2020 elzup
```

Reshaped via `scripts/generate_vocab_assets.py`: each row's `meaning`
column split into a list, entries deduped, and words appearing in more
than one level file assigned to the easiest level they appear in.

`kana.json` is hand-authored (hiragana + katakana, seion + dakuten +
handakuten), not sourced from any dataset.
