# launcher

Simple Maven starter project.

## Coordinates

- Group ID: `dev.nonvocal`
- Artifact ID: `launcher`
- Version: `0.0.1`
- Java release: `26`

## Project structure

- `pom.xml`
- `src/main/java/dev/nonvocal/launcher/LauncherApplication.java`
- `src/test/java/dev/nonvocal/launcher/LauncherApplicationTest.java`

## Build and test

```bash
mvn clean test
```

## Run

```bash
mvn -q -DskipTests package
java -cp target/classes dev.nonvocal.launcher.LauncherApplication
```

