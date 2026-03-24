# LyriSync Android
## Intro
A language learning app that syncs a Spotify song. You can view live translations of each sentence, stream definitions of each phrase, and filter definitions shown through any given Anki decks.

<img width="600" height="800" alt="image" src="https://github.com/user-attachments/assets/e6ef0c72-fb6d-4f0d-81cc-d28929bdf6ef" />

## Features
- Maxmatch search (try to match all words in a sentence, if miss, try a lesser amount) to get the maximum possible accuracy that the database can affrod to provide instead of exact matching (and missing) hiragana / katakana words. Also suffixes to kanji influence the meaning, so this approach is required.
For this reason, db is pushed to RAM to allow fast querying
- Single page UI (activity_main.xml)