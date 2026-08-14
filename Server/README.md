[![AGPL-3.0 License][license-shield]][license-url]

<br />
<p align="center">
  <a href="#comfyscape">
    <img src="https://i.imgur.com/RsfVfkB.png" alt="Logo" width="300" height="67">
  </a>
  <h3 align="center">An open source MMORPG emulation server</h3>

  <h1 id="comfyscape" align="center"><strong>ComfyScape</strong></h1>
  <p align="center">
    <br />
    <br />
    Community Hosted Server
    ·
    ·    Report bugs to the server administrator
  </p>
<h3>NOTE: Bug reports and support are only accepted by/offered to players of our live hosted server. We will not provide support to those running their own copies.</h3>

## Table of Contents

* [Live Server Information](#live-server-information)
* [History of this Codebase](#history-of-this-codebase)
* [Our Core Values](#our-core-values) 
* [Contributing](#contributing)
* [Setup for Content Developers](#content-developers-setting-up-the-project)
  * [GitLab Setup](#gitlab-setup)
  * [Prerequisites](#prerequisites)
  * [Project Setup](#project-setup)
  * [Running the project](#running-the-project)
  * [Browser WebSocket Transport](#browser-websocket-transport)
* [License](#license)
* [Contact](#contact)


## Live Server Information

ComfyScape is a community-hosted server for players who enjoy skilling, questing, and exploring together. Connecting to the live server is also one of the easiest ways to identify bugs, typos, and missing features.

## History of this Codebase

ComfyScape is based on a RuneScape build 530-era server, with content centered around January 1st, 2009.

This project was started out of love for the 530 revision. A small group of developers spent thousands of hours improving on the existing source that was left to the curb. Over the past year, this project has seen many developers coming and going, fixing bugs that they find either through their own server, or bugs that they find in the live game that is currently hosted. We do not accept donations of any kind. The smiles and wonderful compliments are more than enough to keep us going! Content and bugfixes are always number one on our list, and we try our best to answer questions after the readme and frequently asked questions have been reviewed.

## Our Core Values

In the current climate of RuneScape Private Servers in general, we believe it's important to wear our core values on our sleeves and defend them with everything we have! Below are what we hold as our core, most important values:

* **We do NOT believe in profiting off an RSPS.** The ComfyScape team values preserving the spirit of the original game. This is a labor of love and passion for everyone involved.
  

* **Authenticity is central to the work**. As a remake, one of the most important things to us is being true to the Gower spirit. What the Gowers brought to us in our childhood is what we are driven to preserve for the remainder of the world. 


* **Open Source is crucial to the project**. We believe open source remakes to be crucial to preserving what we loved in our childhood, and we believe for-profit and/or closed-source servers are destined to flounder out and fail. 


* **Be welcoming**. One of our most important goals is to provide a community of friendly and caring people that get along and love to enjoy the game with eachother. For this reason, we do tend to be very strict when it comes to toxicity. We care about quality a whole lot more than quantity! 

## Contributing

**Note: All merge requests MUST be made using the defaut MR template. Merge requests that do not use this template will not be accepted.**

**Note: All new contributions MUST be made in Kotlin unless you are updating or fixing a bug in legacy code. More information on Kotlin can be found [here](https://kotlinlang.org/).**

There are many ways everyone can contribute! From the most seasoned programmers to those who do not have the most remote clue how code works! Below are some things that can always use some love from the community.

* **Content Testers**: I'm putting this one up top because of its importance. We, the contributors and developers, aren't perfect. Sometimes, we make mistakes. This is where you come in - If you want a sneak peek at upcoming content, have a knack for breaking things, or just want to contribute to the project without making code changes, you can become a tester!


* **Wiki Editors**: A maintained wiki can be a valuable way to document game content for ComfyScape players.


* **JSON editors**: We could always use more JSON editors. The project tools require Java 11.


* **Authenticity Auditors**: As a remake, authenticity is central to our core values. Auditing one area at a time helps identify opportunities to make the experience closer to the 2009 game.


* **Code Contributors**: As a remake, we have massive amounts of content that need to be implemented or corrected. If you know how to program or are willing to learn, this is where you could be extremely helpful! We need everything from quests, to dialogue, to mini-games, to skills that still need to be corrected. This is perhaps one of the most valuable ways someone could help out the project!

## Content Developers: Setting Up the Project.
### GitLab Setup
**Note: This allows you to commit changes to the main repo with approval. Keep your copy up to date with the latest ComfyScape updates.**

1. Create a GitLab account if you haven't done so already.

2. Follow your team's Git workflow documentation.

**If at anytime you have an issue with GitLab please refer to the [GitLab help center](https://gitlab.com/help).**

### Prerequisites

These are mandatory. If you don't install **all** of these programs **in order** prior to
the project's setup, things won't work. At all.

*For Windows users* - Turn developer mode on first in Windows developer settings.

* [JDK 11](https://adoptium.net) or the Java SE Development Kit Version 11
* [IntelliJ IDEA Community](https://www.jetbrains.com/idea/download/)

### SSH setup

1. [Set up a key if you don't have one (ed25519)](https://docs.gitlab.com/ee/user/ssh.html#generate-an-ssh-key-pair)
2. [Add your public key to your gitlab account](https://docs.gitlab.com/ee/user/ssh.html#add-an-ssh-key-to-your-gitlab-account)
3. [Verify you can connect to git@gitlab.com](https://docs.gitlab.com/ee/user/ssh.html#verify-that-you-can-connect)

### Project Setup

1. If you have not already, follow your team's Git workflow documentation.
2. Run `git lfs pull` in the ComfyScape folder you cloned. This only has to be done once.
3. Follow your team's IntelliJ IDEA setup guide.

### Running the project

1. If you followed the IntelliJ setup guide, which you probably should have, just use the provided run configuration.
***Note: If you choose not to use the provided run scripts or IntelliJ, you *must* run `mvn clean` before it will build correctly.***

#### Linux / OSX

Start the game server with the included run script. Use `./run -h` for more info.

#### Windows

Start the game server with `run-server.bat`

#### Browser WebSocket Transport

Enable the listener in `Server/worldprops/default.conf`:

```properties
websocket_enabled = true
websocket_port = 0
```

When `websocket_port = 0`, the listener uses `53594 + world_id`. For world `1`, that is `53595`.

Both plain WebSocket (`ws://`) and secure WebSocket (`wss://`) are supported. Use `ws://` for local HTTP testing. PWA requires HTTPS/WSS.

Enable WSS with a Java keystore:

```properties
websocket_tls_enabled = true
websocket_tls_keystore_path = "certs/dev-wss.p12"
websocket_tls_keystore_password = "<keystore-password>"
```

The keystore password is optional. Leave `websocket_tls_keystore_password` blank when using a PKCS12 file exported with an empty password:

```bash
openssl pkcs12 -export \
  -in fullchain.pem \
  -inkey privkey.pem \
  -out certs/dev-wss.p12 \
  -name websocket \
  -passout pass:
```

For local WSS development, create a certificate for your hostname or LAN IP that the browser will use, then point the server at a PKCS12 keystore. For production, use a normal certificate from certbot. Plain WS does not require a certificate.

#### Private Windows server

Run `run-server.bat` from the repository root. On first use it creates the ignored local configuration, requests administrator approval to install the `ComfyScapeMariaDB` Windows service, initializes the database, builds the server, and launches it. Later launches reuse the same service and database.

MariaDB listens only on `127.0.0.1:3306`. Do not forward database, management, WebSocket, Remote Desktop, or file-sharing ports. For the complete first-launch and credential-rotation procedure, read `PRODUCTION-HARDENING.md` before exposing the game port.

### License

We use the AGPL 3.0 license, which can be found [here](https://www.gnu.org/licenses/agpl-3.0.en.html). Please be sure to read and understand the license. Failure to follow the guidelines outlined in the license will result in legal action. **We WILL NOT change the license to fit your needs.**

### Contact

**Reminder: Contact the ComfyScape server administrator for support.**


[license-shield]: https://img.shields.io/badge/license-AGPL--3.0-informational
[license-url]: https://www.gnu.org/licenses/agpl-3.0.en.html

### Credits

New accounts receive 2,000 credits. Credits are not earned through donations, voting, or contributions; server owners can grant or remove them at their discretion with `::modcr <player> <amount>`.
