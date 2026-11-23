# Sportsnet

Sportsnet is a Java 17 Spring Boot project combining the shared framework,
resource service, NCAA basketball implementation, and MLB implementation in
`src/main`. The two sport applications are built as separate executable
archives and are selected independently at runtime.

## Build and run

```shell
./gradlew build
./gradlew bootRun
./gradlew bootRunMlb
```

`build` compiles the shared source once and produces
`sportsnet-1.0.0-ncaabb.jar` and `sportsnet-1.0.0-mlb.jar` under
`build/libs/`. The default `bootRun` launches the NCAA basketball application;
`bootRunNcaaBb` is an explicit alias, and `bootRunMlb` launches the MLB
application. Each listens on port `8287` by default, so run only one on that
port at a time (or override `server.port` to use different ports).

Both applications use the shared framework and DeepLearning4J with the ND4J
native backend. Gradle resolves the platform-specific backend at runtime.
