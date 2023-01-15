## Minesumo [![CodeFactor](https://www.codefactor.io/repository/github/goldengamerlp/minesumo/badge)](https://www.codefactor.io/repository/github/goldengamerlp/minesumo)

# Todo

- Playermanager
- Manage Instances (Ready)
- Basic Class for a match
- Create a new instance with a mapconfig and when a instance already exists, shared instance

- teams = spawns
- maxPlayers = playerPerTeam * playerSpawns
- alternative = not using playerPerTeams

# Two different scoreboards, team scoreboard, 1v1 scoreboard

# The Game has to have a maximum time

# No lobby, the map is choosen automatically

- Procedure:

1. Wait for players. Countdown if min players (1 player per spawn) are there.

- Can move, cant hit, lobby scoreboard

2. Game Start. Countdown from 3. Start the game and everyone can hit each other.

- Can move, can hit, ingame scoreboard

3.InGame.

- If 1v1 Game = Scoreboard ping, hits, cps
- If Other = Team, eliminated players

- Die if the death pos is reached.

4. End

- Countdown from 10. Teleport back to main instance
- 
