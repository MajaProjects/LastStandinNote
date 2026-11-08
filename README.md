# LastStandinNote

A rhythm game inspired by *Friday Night Funkin’*, featuring pixel art and a musical showdown between **Tux** and **Windows**. Follow the arrows, hit notes to the beat, and hold sustained notes while Kernel reacts to the performance on stage.

Packaged versions are distributed through [GitHub Releases](https://github.com/MajaProjects/LastStandinNote/releases).

## How to play

Notes scroll down toward the receptors at the bottom of the screen. Press the key for a column when the note head reaches its receptor.

- **Tap notes:** press the key when the head reaches the receptor.
- **Sustained notes:** press at the start and hold until the tail ends.
- **Multiple columns:** keep holding one note while playing notes in other columns.

Each hit receives a judgement based on how close the input is to the note's time. Misses affect health, interrupt the combo, and produce visual and sound feedback. Character vocals follow successful hits and are muted on misses, while the instrumental continues playing.

Notes and vocals follow the audio clock. Changing the FPS limit affects drawing frequency, not song speed.

## Menu flow

1. Press **Enter** on the title screen.
2. Choose **Play**, then select a game mode.
3. Select a song with **up and down**.
4. Change its difficulty with **left and right** on the song selection screen.
5. Choose your character and confirm with **Enter**.
6. Get ready during the countdown and play the song.

The main menu also includes **Options** and **Credits**. Pressing **Escape** on the title screen opens the quit confirmation.

## Characters

| Character | Stage position | Role |
| --- | --- | --- |
| Windows | Left | Playable character or opponent |
| Tux | Right | Playable character or opponent |
| Kernel | Center, on the monitor | Reacts to combos and combo breaks |

Character selection determines who each participant controls. Tux stays on the right and Windows on the left. The center monitor shows who leads the shared health bar; when both sides are even, it shows the matchup.

## Game modes

### Single player

Control your chosen character while the bot plays the other side. The controls are always **D F J K**, whether you choose Tux or Windows.

The bot varies its accuracy and can make mistakes. When you play well and maintain a combo, it can increase its pressure to keep the song challenging.

If your health runs out, a defeat animation plays before the Game Over screen. You can restart or return to song selection. Completing the song shows only your character and statistics, without declaring a winner or loser.

### Two players on one computer

Both players share the keyboard. Controls are attached to each character:

| Character | Left | Down | Up | Right |
| --- | --- | --- | --- | --- |
| Windows | A | S | D | F |
| Tux | J | K | L | Ñ or ; |

The song continues until the end, even if one side of the health bar reaches zero. Results compare both players. Accuracy is calculated relative to each side's notes, so a larger chart does not automatically win. If accuracy is tied, points per note break the tie.

Use either **Ñ** or **semicolon (`;`)** for Tux's right arrow, depending on your keyboard. Both keys control the same column and work for sustained notes and resuming after a pause. Single-player controls remain **D F J K**.

## Songs and difficulties

The available songs are **Puzzle**, **Buy Something**, and **Melancholy**. Each has three note charts:

| Difficulty | Experience |
| --- | --- |
| USER | Entry level, with slower scrolling |
| ADMIN | Intermediate challenge, with its own chart |
| ROOT | Faster scrolling and effects that make notes harder to read |

On **ROOT**, notes can sway sideways, fade, and be accompanied by stage shakes. Their position and visibility recover near the receptor so the timing remains readable.

Change difficulty directly with left and right on the song selection screen. The segmented indicator shows the selected level.

### Local single-player records

Selecting a song with a record shows its best score and best accuracy for the chosen difficulty. These two personal bests are saved independently between sessions: a better score does not overwrite a higher accuracy from another run. Only completed human single-player runs count. Bot and two-player performances are excluded. Two-player mode retains its results screen but does not show solo records in song selection.

A colored badge appears beside songs with recorded accuracy: **P** for exactly 100%, **S** for above 95%, **A** for 90–95%, **B** for 80–below 90%, **C** for 70–below 80%, and **D** for below 70%. USER, ADMIN, and ROOT each have separate records. Unplayed songs show no record or badge.

## General controls

| Action | Control |
| --- | --- |
| Move between menu options or songs | Up / down |
| Change difficulty, character, or option value | Left / right |
| Confirm a selection | Enter |
| Return to the previous menu | Escape |
| Pause or resume a song | P |
| Toggle fullscreen | F11 |
| Open the confirmation to leave an active song | Escape during gameplay |
| Cancel an exit confirmation | Escape |

In single player, **D**, **F**, **J**, and **K** correspond to left, down, up, and right.

## Judgements, accuracy, and score

**Score** and **accuracy** measure different things. Score depends on how close an input is to the exact note time. Accuracy uses each note's judgement weight:

| Judgement | Distance from the note's time | Accuracy weight |
| --- | --- | --- |
| SICK | Up to 45 ms early or late | 100% |
| GOOD | Over 45 and up to 90 ms | 100% |
| BAD | Over 90 and up to 135 ms | 50% |
| SHIT | Over 135 and up to 160 ms | 25% |
| MISS | Missed note or a sustain released too early | 0% |

**GOOD grants full accuracy**, while awarding fewer points than a more precisely timed hit. A sustained note counts once when completed; releasing it too early can turn it into a miss.

The combo counts consecutive successful notes. **BAD** and **MISS** break it. Combo milestones depend on the number of notes on your side and trigger an animation and sound when reached by a human player.

## Health and results

The bottom bar is shared: Windows owns its left portion and Tux its right portion. Hits and mistakes shift the balance between them. In two-player mode, note influence is adjusted to compensate for differences in chart size.

When a song ends, the results screen quickly counts up and displays:

- Total notes and hit notes.
- Counts of SICK, GOOD, BAD, SHIT, and MISS.
- Maximum combo, final score, and accuracy.
- Options to restart or return to song selection.

Single-player results show the selected character's performance. Two-player results show both columns and the match outcome.

## Pause and exit

Press **P** to pause, resume, restart, or return to song selection. Music and notes stop together. Resuming preserves the song position and does not replay the opening countdown.

If you paused during a sustained note, the game may ask you to hold the corresponding keys before resuming. Switching to another window also pauses the song.

Leaving an active song or closing the game opens a confirmation to prevent accidental progress loss.

## Options

| Option | Available settings |
| --- | --- |
| Resolution | 960 × 540, 1280 × 720, or 1920 × 1080 |
| Music volume | Menu music and instrumentals |
| Vocal volume | Character vocals |
| Effects volume | Interface sounds, misses, and alerts |
| Audio offset | −200 to +200 ms, in 5 ms steps |
| Window mode | Windowed or fullscreen |
| Language | English or Spanish |
| FPS limit | 30, 60, 120, 244, or a custom value |
| Show FPS | Frames-per-second counter |
| Show game stats | Score, accuracy, combo, and misses during gameplay |

The custom FPS slider runs from 0 to 260 and ends with **Unlimited**. Its lowest position uses an effective minimum of 1 FPS so the screen keeps updating.

Option changes remain pending until you choose **Confirm and back**. Leaving with unsaved changes lets you save them, discard them, or keep editing. Confirmed settings are retained when you reopen the game.

On first launch, the game uses Spanish if the system language is Spanish; otherwise it uses English. A language saved in Options takes priority.

The language setting changes menus, prompts, judgements, and results. Song names, character names, and author names retain their original spelling.

## Adjusting audio offset

If your inputs arrive late relative to what you hear, try a negative value, such as −50 ms. If they arrive too early, try a positive value.

The setting moves arrow timing, judgements, sustains, and bot timing together. It does not change playback speed or modify audio files. Start near zero and confirm the setting in Options before trying another song. Pressing Enter on this option resets the pending value to zero.

## Version and error reports

The version appears on the title screen and in Credits. If an unhandled error occurs, the game displays a message and closes. Errors are saved to `~/.laststandinnote/log.txt` inside your user folder, with a size-limited previous log to prevent indefinite growth. Attach that file when reporting a problem.

## Project structure

The game is written in Java with Swing and loads its images, audio, and note charts from resource files.

| Folder | Contents |
| --- | --- |
| `src/` | Screens, game rules, audio, and settings |
| `resources/images/` | Characters, stages, arrows, interface, bitmap font, and window icons |
| `resources/audio/` | Menu music and sound effects |
| `resources/songs/` | Songs, difficulty charts, and vocals |
| `resources/CREDITS.txt` | Resource credits, sources, and attribution |
| `packaging/` | Native launcher icons |
| `tools/` | Validation and packaging tools |
| `docs/` | Maintainer documentation |

## License

The original Java source code in `src/` is released under the **GNU General Public License, version 3 only** (`GPL-3.0-only`). The full text is in [LICENSE](LICENSE); its scope and copyright notices are recorded in [NOTICE.md](NOTICE.md).

You may study, modify, and redistribute the code under GPLv3. Distributing modified versions requires retaining the applicable notices and making the corresponding source code available under that license.

Character sprites, bitmap lettering, character vocals, and note charts created and edited by the team are available under **Creative Commons Attribution-ShareAlike 4.0** (`CC-BY-SA-4.0`). See [LICENSE-ASSETS](LICENSE-ASSETS) for the full text and [NOTICE.md](NOTICE.md) for its scope. The code retains its GPLv3 license, and instrumentals retain their original licenses.

Third-party instrumentals, backgrounds, icons, and sound effects retain their own licenses. Being placed in `resources/` does not assign a resource a new license. See the attribution and terms for each resource in [CREDITS.txt](resources/CREDITS.txt).

## Team and credits

| Team member | Contributions |
| --- | --- |
| Jacobo Parada Urriago | Code and music |
| Manuela Gutierrez Diaz | Sprites, lettering, and code |

External authors and sources appear in the in-game credits and [CREDITS.txt](resources/CREDITS.txt). Character vocals were created and edited by the team. Instrumentals retain their original creators' authorship and licenses:

| Song | Original instrumental | Author | License |
| --- | --- | --- | --- |
| Puzzle | [Puzzle Menu](https://opengameart.org/content/puzzle-menu) | caret7 | CC BY 4.0 |
| Buy Something | [Shop Theme](https://opengameart.org/content/shop-theme) | Cleyton Kauffman | CC0 1.0 |
| Melancholy | [Melancholy RPG](https://opengameart.org/content/melancholy-rpg) | Alexandr Zhelanov | CC BY 3.0 |

## Downloads and bug reports

Published builds are available through [GitHub Releases](https://github.com/MajaProjects/LastStandinNote/releases).

Report bugs in [Issues](https://github.com/MajaProjects/LastStandinNote/issues). Include the song, difficulty, mode, character, operating system, and steps to reproduce the problem. A screenshot or error message helps identify the cause.

Main repository: [MajaProjects/LastStandinNote](https://github.com/MajaProjects/LastStandinNote).

Project mirror: [Last Standin Note on GitLab](https://gitlab.com/GLUD/gnubies/proyectos-2023-iii/2026-iii/gnubies-martes-8-10am/last-standin-note).
