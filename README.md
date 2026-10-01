# Create Mekanism Zinc

A private, independently buildable addon for Minecraft 1.21.1, NeoForge 21.1.252, Create 6.0.10, and Mekanism 10.7.19.85. The addon is at an early development stage; its assets and content are not release-ready. No Minecraft GUI or authenticated client verification has been performed.

## Build

Use JDK 21. Gradle 9.2.1 is provided by the wrapper.

```sh
./gradlew :common:test :neoforge:build
```

The `common` module contains loader-independent code and assets. `neoforge` contains game integration. The ordinary NeoForge jar includes common outputs and does not bundle Create, Mekanism, or other dependency jars.

Development runs use ignored `.local/` working directories. Available tasks include `:neoforge:runClient`, `:neoforge:runServer`, `:neoforge:runData`, and `:neoforge:runGameTestServer`.
