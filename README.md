# LyriSync Android
## Intro
A language learning app that syncs a Spotify song. You can view live translations of each sentence, stream definitions of each phrase, and filter definitions shown through any given Anki decks.

![Screenshot_20260324_112631_LyriSync](https://github.com/user-attachments/assets/6a724fd0-61a9-4030-ac02-9c444f89c113)

## Features
- Maxmatch search (try to match all words in a sentence, if miss, try a lesser amount) to get the maximum possible accuracy that the database can affrod to provide instead of exact matching (and missing) hiragana / katakana words. Also suffixes to kanji influence the meaning, so this approach is required.
For this reason, db is pushed to RAM to allow fast querying
- Single page UI (activity_main.xml)
