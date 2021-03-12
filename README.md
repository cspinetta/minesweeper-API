# minesweeper-API
API for Minesweeper game

The development is guided by the following instructions: [INSTRUCTIONS.md](INSTRUCTIONS.md)

## Dependencies:
- [Scala] v2.13.x
- [Play framework] v2.8.x

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

## TODO list
- [x] Initial Setup
- [ ] API
- [ ] Logic
- [ ] Functional tests
- [ ] Doc + Swagger
- [ ] Contextual logging
- [ ] Metrics
- [ ] Tracing
- [ ] Playground with a docker compose
- [ ] Architecture decision record 


[Scala]: https://www.scala-lang.org/
[Play framework]: https://www.playframework.com/
