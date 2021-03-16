# minesweeper-API
API for Minesweeper game

The development is guided by the following instructions: [INSTRUCTIONS.md](INSTRUCTIONS.md)

## Dependencies:
- [Scala] v2.13.x
- [Play framework] v2.8.x

## Minesweeper-CLI

`minesweeper-cli` is a user-friendly command-line interface to interact and play the Minesweeper.

Go to the sub-folder [minesweeper-cli](./minesweeper-cli)

![Minesweeper CLI](docs/minsweeper-cli_v1.png)

## API

This app uses Basic Access Authentication. More info at https://en.wikipedia.org/wiki/Basic_access_authentication

**You can explore the Swagger UI:**

![Swagger UI](docs/swagger-api-doc_v2.png)

- At localhost:

http://localhost:9000/docs/swagger-ui/index.html?url=/assets/swagger.json#/

- At production:

https://cspinetta-minesweeper-api.herokuapp.com/docs/swagger-ui/index.html?url=/assets/swagger.json

**Or pick a curl:**

* **/health-check**

````shell script
curl -X GET https://cspinetta-minesweeper-api.herokuapp.com/health-check
````

* **/players**

**Create:**

````shell script
curl --request POST \
  --url https://cspinetta-minesweeper-api.herokuapp.com/player \
  --header 'Content-Type: application/json' \
  --data '{
	"username": "<username>",
	"password": "<password>"
}'
````

**Get by ID:**

````shell script
curl --request GET \
  --url https://cspinetta-minesweeper-api.herokuapp.com/player \
  --header 'Authorization: Basic <credentials>'
````

**Delete by ID:**

````shell script
curl --request DELETE \
  --url https://cspinetta-minesweeper-api.herokuapp.com/player/2 \
  --header 'Authorization: Basic <credentials>'
````

* **/games**

**Create a new one:**

````shell script
curl --request POST \
  --url https://cspinetta-minesweeper-api.herokuapp.com/games \
  --header 'Content-Type: application/json' \
  --header 'Authorization: Basic <credentials>' \
  --data '{
	"playerId": 1,
	"height": 10,
	"width": 10,
	"mines": 4
}'
````

**Get by ID:**

````shell script
curl --request GET \
  --url https://cspinetta-minesweeper-api.herokuapp.com/games/1 \
  --header 'Authorization: Basic <credentials>'
````

**Reveal a cell:**

````shell script
curl --request PATCH \
  --url https://cspinetta-minesweeper-api.herokuapp.com/games/1 \
  --header 'Content-Type: application/json' \
  --header 'Authorization: Basic <credentials>' \ 
  --data '{
	"action": "reveal",
	"position": {
		"x": 8,
		"y": 10
	}
}'
````

**Set a question flag:**

````shell script
curl --request PATCH \
  --url https://cspinetta-minesweeper-api.herokuapp.com/games/1 \
  --header 'Content-Type: application/json' \
  --header 'Authorization: Basic <credentials>' \ 
  --data '{
	"action": "set-question-flag",
	"position": {
		"x": 8,
		"y": 10
	}
}'
````

**Set a red flag:**

````shell script
curl --request PATCH \
  --url https://cspinetta-minesweeper-api.herokuapp.com/games/1 \
  --header 'Content-Type: application/json' \
  --header 'Authorization: Basic <credentials>' \ 
  --data '{
	"action": "set-red-flag",
	"position": {
		"x": 8,
		"y": 10
	}
}'
````

**Clean a cell:**

````shell script
curl --request PATCH \
  --url https://cspinetta-minesweeper-api.herokuapp.com/games/1 \
  --header 'Content-Type: application/json' \
  --header 'Authorization: Basic <credentials>' \ 
  --data '{
	"action": "clean",
	"position": {
		"x": 8,
		"y": 10
	}
}'
````

## Development process

### Unit tests

````sbtshell
sbt clean test
````

### Run in Dev mode

````sbtshell
sbt clean run
````

### Coverage

````sbtshell
sbt clean coverage test coverageReport
````

## Release and Deployment process

````shell script
./release.sh 0.0.1
./deploy.sh 0.0.1
````

## TODO list

1. [x] Initial Setup
2. [x] Release and Deployment process
3. [x] Core functionalities (listed below)
4. [x] Authentication
5. [x] Functional tests
6. [x] Doc + Swagger
7. [x] Release MVP in a platform
8. [ ] Contextual logging
9. [ ] Metrics
10. [ ] Tracing
11. [ ] Playground with a docker compose
12. [ ] Architecture decision record

**Note:** the goal is to cover the first 7 points in a week
to get a productive MVP with the required functionalities.

### Core functionalities

- [x] Design and implement a documented RESTful API for the game (think of a mobile app for your API)
- [x] Implement an API client library for the API designed above. Ideally, in a different language, of your preference, to the one used for the API
- [x] When a cell with no adjacent mines is revealed, all adjacent squares will be revealed (and repeat)
- [x] Ability to 'flag' a cell with a question mark or red flag
- [x] Detect when game is over
- [x] Persistence
- [x] Time tracking
- [x] Ability to start a new game and preserve/resume the old ones
- [x] Ability to select the game parameters: number of rows, columns, and mines
- [x] Ability to support multiple users/accounts

### Basic functional flow

![Basic functional flow](docs/minisweeper_basic-functional-flow_v1.png)

[Scala]: https://www.scala-lang.org/
[Play framework]: https://www.playframework.com/
