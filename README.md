# Sportsnet

Sportsnet is a Java 17 Spring Boot application combining the framework, resource
service, and NCAA basketball sources in `src/main`.

## Build and run

```shell
./gradlew build
./gradlew bootRun
```

The application starts on port `8287` by default. Gradle's `bootRun` task launches
`com.jstevenperry.sportsnet.ncaabb.SportsnetNcaaBbApplication`.

The application uses DeepLearning4J with the ND4J native backend. Gradle resolves
the platform-specific backend at runtime.
