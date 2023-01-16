# Minesumo [![CodeFactor](https://www.codefactor.io/repository/github/goldengamerlp/minesumo/badge)](https://www.codefactor.io/repository/github/goldengamerlp/minesumo) [![Java CI with Gradle and Upload](https://github.com/GoldenGamerLP/Minesumo/actions/workflows/gradle_build.yml/badge.svg)](https://github.com/GoldenGamerLP/Minesumo/actions/workflows/gradle_build.yml)

## Abstract

This is a Minecraft plugin for Sumo tournaments. The plugin was developed for [Minestom](https://github.com/Minestom/Minestom).


## Features

- [x] Automatic Arena Queuing
- [x] Automatic Arena Selection
- [x] Full Players and Arena statistics
- [x] Async Programming in mind
- [x] Easy Party Implementation
- [x] Customizable Arenas
- [x] Customizable Messages (currently only German, English! Read more below)
- [x] [MongoDB Support](https://github.com/mongodb/mongo-java-driver) (for Stats)
- [x] **Easy to setup!**

## Setup
1. Download the lastest version of the [**extension**](https://github.com/GoldenGamerLP/Minesumo/actions)
2. Use a **Minestom** Server of your choice
3. Put the extension in the **extensions** folder
4. **Start the server**
5. Stop the Server and edit the **config.json** in the **extensions/minesumo** folder. Put the MongoDB String and put "isInEditorMode" to true.
6. Start the server again
7. Get some Schematics of your choice and put them in the **extensions/minesumo/schematics** folder. **Make sure the schematics are Sponge-schematics!!!**
8. Log into the Server and type **/setup join <yourSchematic.schem>**. This will create a new arena with the name of the schematic. You can now edit the arena.
   - Make sure to:
   - Set the **spectator spawn** of the arena. Via **/setup spectator set**
   - Add any spawn. Via **/setup spawn add**. Or remove any Spawn. Via **/setup spawn remove <id>**
   - Set the death-height of the arena. Via **/setup deathheight set <height>**
   - When done editing the arena, type **/setup SAVE**. This will save the arena, and you can now use it. The Server stops to save.
9. Repeat step 7 for every arena you want to use.
10. Edit the **config.json** again. Set "isInEditorMode" to false.

## Commands
1. _**/setup**_ - The setup command. Use **/setup** to setup maps. Only available in editor mode.
2. _**/start**_ - Starts a Sumo match. Configure a custom amount of players in the config to start a match.
3. _**/lives <number>**_ - Sets the amount of lives you have. Default is 3. Range and Default editable.
4. _**/stats [player] | game [gameID]**_ - Shows your stats or the stats of a player or arena.
5. _**/gameid**_ - Shows the ID of the current game you are in.
6. _**/debug**_ - Use on _your own risk_.

## Messages
- The messages are currently only available in German and English.
- You cannot edit the messages until you edit the **resources** in the classpath.

## Pictures

##### In-Game
<img src="https://i.imgur.com/b4DtMrD.png" width="40%" height="50%" alt="">

##### Player Stats
<img src="https://i.imgur.com/Oixlfhd.png" width="40%" height="120%" alt="">

##### Arena Stats

<img src="https://i.imgur.com/upDoexO.png" width="40%" height="20%" alt="">

##### Main Configuration

<img src="https://i.imgur.com/l5iDHp0.png" width="30%" height="20%" alt="">

##### Example Map Configuration

<img src="https://i.imgur.com/vZU8FIc.png" width="30%" height="20%" alt="">

##### Folders created by Minesumo

<img src="https://i.imgur.com/blKLzty.png" width="35%" height="20%" alt="">

# Footnote 
I worked about one month on this project. I hope you enjoy it. If you have any questions, feel free to contact me on Discord: **GoldenGamerLP#1404**
If you have any suggestions, feel free to open an issue or contact me on Discord.

# License 
This project is licensed under the GNU GENERAL PUBLIC LICENSE Version 3.
