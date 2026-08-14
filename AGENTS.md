# ComfyScape workspace guidance

## Workspace scope

- This IntelliJ workspace is `D:\OSRS\ComfyScape`, a Git monorepo containing `Server/` and `Client/`. Run Git commands from the workspace root.
- Do not access or modify anything outside this workspace, including other experiments in `D:\OSRS`.
- `Server/` is the 2009scape game and management server. Its Maven `pom.xml` is at `Server/Server/pom.xml`; do not assume `Server/` itself is the Maven root.
- `Client/` is the maintained 2009scape RT4 client and uses Gradle.
- `Server/Tools/Frostys Cache Editor` is a separate Eclipse tool, outside the core server build and not a current priority.
- `distribution/`, when created, will contain packaging scripts, a portable JRE 11, and friend-facing ZIP output.

## Project decisions

- Do not reintroduce the Saradomin launcher for distribution: it cannot target a custom server host.
- Distribution should use the raw RT4 client with a pre-edited server configuration and a portable JRE 11, so players can launch it without manual configuration.
- Use the official `2009scape/singleplayer/windows` `crossplatform-release` project only as a packaging reference; do not build from it directly.
- The public server hostname is `danny-games.servegame.com`. Never hardcode localhost or a LAN IP in client or server configuration.
- Confirm the game port from the server configuration before using it anywhere; do not assume a port number.
- Treat client server-address or port changes as deliberate packaging changes and clearly call them out.
- Keep the intentional build-tool split: Maven for the server and Gradle for the client.

## Generated and low-value files

- Do not inspect, search, edit, or commit generated build outputs unless the task explicitly requires them: `.gradle/`, `build/`, `target/`, `*.class`, and packaged Java archives (`*.jar`, `*.war`, `*.ear`, `*.rar`).
- Avoid IDE metadata and indexing files: `.pki/`, `.metadata/`, `*.ipr`, `*.iws`, and `*.iml`.
- Prefer source, configuration, and build-definition files when investigating or changing behavior.
