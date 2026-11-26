# SandFly User Guide sources

The SandFly User Guide opens from **Help & feedback** > **User guide** in SandFly settings. SandFly builds one page from the files in this folder, with a table of contents, section numbers and links.

**Help** in the app bar of a settings screen opens the section about that screen: the heading and the headings under it. `SettingsGuideSections` in `talkback/.../userguide/` maps each screen to the ID of its heading; screens without an entry have no **Help**. When you change or remove a heading ID, update that map. `GuideMarkdownTest` checks that every mapped ID exists in each guide.

## Files

- `en/` holds the English guide. Each other language has a folder of its own, named like the Android resource folders (`de`, `pt-rBR`, `zh-rCN`), with files of the same names. A file missing from a language folder is shown in English.
- Each file is one chapter. Chapters are in the order of their file names: files that start with a number (`01-getting-started.md`) are numbered chapters, files that start with `appendix-` are appendices, lettered A, B, C. The numbers in file names only set the order; the guide numbers chapters by their place.
- To add a chapter, add a file. To move one, rename it.
- The unit test `GuideMarkdownTest` checks the English guide for headings without IDs, IDs used twice and links to headings that don't exist.

## Headings

- A file starts with one level 2 heading (`##`), the chapter title. Sections are level 3 (`###`) and subsections level 4 (`####`). Don't use `#` or `#####`.
- Every heading ends with an ID in braces: `### Change the speech rate {#speech-rate}`. IDs are lowercase words joined with hyphens. A section ID starts with the ID of its chapter (`speech-…` in the Speech chapter), so that IDs stay unique across files. Translations keep the IDs.
- Don't number headings. SandFly numbers them: 1, 1.1, 1.1.1 in chapters, A, A.1, A.1.1 in appendices.

## Links

- `[](#speech-rate)` links to a heading and shows its number and title, such as "5.2 Change the speech rate". Use it for "see" references: `See [](#speech-rate).`
- `[text](#speech-rate)` links to a heading with your own text.
- `[text](https://…)` opens a web page in the browser.

## Text

- Paragraphs are separated by a blank line.
- Lists start with `- ` or `1. `. Indent a list item two spaces to put it inside the item above.
- Tables have a header row and a `| --- |` row.
- `**Bold**` marks the name of a setting, button, command, gesture or other item on screen, exactly as SandFly shows it.
- Settings paths use ` > `: `**Speech** > **Voice profiles**`. The guide shows a › between the names and reads a comma.
- `` `code` `` marks text to type, file names, and code.
- Blocks of code start and end with a line of three backticks.
- A paragraph that starts with `Note: ` is shown as a note.
- `<!-- generate: name -->` on a line of its own inserts a list that SandFly makes from its own defaults, in the language of the guide: `gestures` or `keyboard-shortcuts`.
- `<!-- include: file.md -->` on a line of its own inserts a document from `docs/`, such as `scripting.md`. Its title is left out and its headings move one level down. Links inside it to its own headings and to the other included documents keep working. Documents that can be included are listed in the `userGuideAssets` task in `talkback/build.gradle`.
- A language folder can supply a translation of an included document under the same file name. Add `{#original-anchor}` to each translated heading so that it keeps the original link target. Use `#original-anchor` in links within the document.
- Other HTML is shown as text.

## Style

The guide follows the Apple Style Guide.

- Write in second person, present tense and active voice: "You can …", "To …, go to …".
- Name tasks with imperative headings ("Change the speech rate", "Add a gesture profile") and concepts with noun phrases ("Reading controls").
- Steps are numbered lists, one action each.
- Name every setting, command and gesture exactly as SandFly shows it, and give its full path from the main SandFly settings screen.
- Gestures in running text: "swipe right", "double-tap", "triple-tap", "two-finger swipe up", "touch and hold". In names of settings and gestures, use the names SandFly shows ("Swipe up then right").
- State what happens: SandFly reads, announces, plays a sound, vibrates, moves focus, opens a menu. Things don't know, ask, want, listen or have opinions.
- No possessives on things: "the name of the app", not "the app's name".
- No filler, reassurance or marketing words ("simply", "easily", "powerful", "just").
- Short paragraphs. Explain why only where a choice depends on it.
