>[!IMPORTANT]
>**🎨 Material 3 Expressive Fork of [Ivy Wallet](https://github.com/Ivy-Apps/ivy-wallet)**
>
>This repository is a fork of Ivy Wallet dedicated to refreshing the user interface using **Material 3 Expressive** design system.
>
>- **UI-Only Redesign**: Focuses purely on modernizing the user interface, layout, animations, and design tokens with Material 3 Expressive.
>- **100% Data & Feature Compatibility**: Preserves all core money management features, business logic, and database structures exactly as in the original Ivy Wallet.
>- **Seamless Interoperability**: Users can freely switch back and forth between the original Ivy Wallet and this fork without data loss.
>- **Project Status**: The upstream repository was archived by the original maintainers on **Nov 5th, 2024**. Development continues independently here under the [GPL-3.0 License](LICENSE).

[![Latest Release](https://img.shields.io/github/v/release/Ivy-Apps/ivy-wallet)](https://github.com/Ivy-Apps/ivy-wallet/releases)
[![APK](https://github.com/Ivy-Apps/ivy-wallet/actions/workflows/apk.yml/badge.svg)](https://github.com/Ivy-Apps/ivy-wallet/actions/workflows/apk.yml)
[![Telegram Group](https://img.shields.io/badge/Telegram-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/+ETavgioAvWg4NThk)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![GitHub Repo stars](https://img.shields.io/github/stars/Ivy-Apps/ivy-wallet?style=social)](https://github.com/Ivy-Apps/ivy-wallet/stargazers)
[![Fork Ivy Wallet](https://img.shields.io/github/forks/Ivy-Apps/ivy-wallet?logo=github&style=social)](https://github.com/Ivy-Apps/ivy-wallet/fork)

# Ivy Wallet: money manager

|                                                                                                            |                                                                                                            |                                                                                                            |                                                                                                            |
|:----------------------------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------------------------:|
| ![1](https://user-images.githubusercontent.com/5564499/189540998-4d6cdcd3-ab4d-40f7-85d4-c82fe8a017d1.png) | ![2](https://user-images.githubusercontent.com/5564499/189541011-1ebbd8b6-50fe-432a-91e2-59206efe99ce.png) | ![3](https://user-images.githubusercontent.com/5564499/189541023-35e7f163-d639-4466-9a91-c56890d5a28e.png) | ![4](https://user-images.githubusercontent.com/5564499/189541027-d352314c-fd5c-43eb-82ad-4aba14c7b0fa.png) |
| ![5](https://user-images.githubusercontent.com/5564499/189541030-1a0d7948-33af-420b-b126-936d0211c93f.png) | ![6](https://user-images.githubusercontent.com/5564499/189541035-621c4511-5ec7-4d3f-b08e-925d8da95472.png) | ![7](https://user-images.githubusercontent.com/5564499/189541127-7adf5bfa-0652-461c-80f1-076b7179eb6c.png) | ![8](https://user-images.githubusercontent.com/5564499/189541040-7cab633e-be4c-40b2-a2c6-890a15edf805.png) |

Ivy Wallet is a free and open source **money management android app**. It's written using **100% Kotlin and Jetpack Compose**. It's designed to help you keep track of your personal finances with ease.

Think of Ivy Wallet as a manual expense tracker that tries to replace the good old spreadsheet for managing your finances.

**Do you know? Ask yourself.**

1) How much money do I have in total?

2) How much did I spend this month and what did I spend it on?

3) How much can I spend and still meet my financial goals?

A money management app can help you answer these questions.

Ivy Wallet may lack some of the features you're looking for, but it truly shines in its user interface and experience, as well as its simplicity and customization options. This was recognized in the ["Top/Best Android App in 2021/2022 charts"](https://youtube.com/playlist?list=PLguJN0waG1-eSzKMuFMIULrR3MlqJ3cAE) by the YouTube tech community.

> To support our free open source project, please give it a star. ⭐
> This means a lot to us. Thank you so much! [![GitHub Repo stars](https://img.shields.io/github/stars/Ivy-Apps/ivy-wallet?style=social)](https://github.com/Ivy-Apps/ivy-wallet/stargazers)

## Project Requirements

- Java 17+
- The **latest stable** Android Studio (for easy install use [JetBrains Toolbox](https://www.jetbrains.com/toolbox-app/))

### Initialize the project

**1. Fork and clone the repo**

Instructions in [CONTRIBUTING.md](./CONTRIBUTING.md).

## Learning Materials

Ivy Wallet is a great place to code and learn. That's why we also link to great learning materials (books, articles, videos), check them out in **[docs/resources 📚](docs/resources/)**.

Make sure to check out our short **[Developer Guidelines 🏗️](docs/Guidelines.md)** to learn more about the technical side of the Ivy Wallet.

## Tech Stack

### Core

- 100% [Kotlin](https://kotlinlang.org/)
- 100% [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material3 design](https://m3.material.io/) (UI components)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) (structured concurrency)
- [Kotlin Flow](https://kotlinlang.org/docs/flow.html) (reactive data stream)
- [Hilt](https://dagger.dev/hilt/) (DI)
- [ArrowKt](https://arrow-kt.io/) (functional programming)


### Testing
- [JUnit4](https://github.com/junit-team/junit4) (test framework, compatible with Android)
- [Kotest](https://kotest.io/) (unit test assertions)
- [Paparazzi](https://github.com/cashapp/paparazzi) (screenshot testing)

### Local Persistence
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (key-value storage)
- [Room DB](https://developer.android.com/training/data-storage/room) (SQLite ORM)

### Networking
- [Ktor client](https://ktor.io/docs/getting-started-ktor-client.html) (HTTP client)
- [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) (JSON serialization)

### Build & CI
- [Gradle KTS](https://docs.gradle.org/current/userguide/kotlin_dsl.html) (Kotlin DSL)
- [Gradle convention plugins](https://docs.gradle.org/current/samples/sample_convention_plugins.html) (build logic)
- [Gradle version catalogs](https://developer.android.com/build/migrate-to-catalogs) (dependencies versions)
- [GitHub Actions](https://github.com/Ivy-Apps/ivy-wallet/actions) (CI/CD)
- [Fastlane](https://fastlane.tools/) (uploads the app to the Google Play Store)

### Other
- [Firebase Crashlytics](https://firebase.google.com/products/crashlytics) (stability monitoring)
- [Timber](https://github.com/JakeWharton/timber) (logging)
- [Detekt](https://github.com/detekt/detekt) (linter)
- [Ktlint](https://github.com/pinterest/ktlint) (linter)
- [Slack's compose-lints](https://slackhq.github.io/compose-lints/) (linter)

## Contribute

**Want to contribute?** See **[CONTRIBUTING.md](/CONTRIBUTING.md)** [![Fork Ivy Wallet](https://img.shields.io/github/forks/Ivy-Apps/ivy-wallet?logo=github&style=social)](https://github.com/Ivy-Apps/ivy-wallet/fork)

### Contributors Wall:

<a href="https://github.com/ILIYANGERMANOV/ivy-wallet/graphs/contributors">
  <img alt="contributors graph" src="https://contrib.rocks/image?repo=Ivy-Apps/ivy-wallet" />
</a>
<br>
<br>

_Note: It may take up to 24 hours for the [contrib.rocks](https://contrib.rocks/preview?repo=Ivy-Apps%2Fivy-wallet) plugin to update._ 

**P.S.** You'll also be recognized in a special "Contributors" section. We salute you! 👏

## Creative Contributors

Folks that helped Ivy Wallet in a non-dev creative ways that can't be captured on GitHub.

### Creative Contributors Wall:

<!-- <div align="center">
  <a href="URL_TO_CONTRIBUTION">
    <img src="URL_TO_PERSONS_PHOTO" width="100px;" alt="PERSON'S PHOTO"/><br>
    <strong>USERNAME</strong><br>
    <small>MESSAGE_FOR_THEIR_CONTRIBUTION</small>
  </a>
</div> -->

<div style="text-align: center">
    <img src="https://avatars.githubusercontent.com/u/62771583?v=4" width="100px;" alt="Stefan Ilijev - Desinger"/><br>
    <strong>Stefan Ilijev</strong><br>
    <small>Co-founder and designer of Ivy Wallet. Created the <a href="https://www.figma.com/file/kSwIa07jcHEHZXo6rzx7dn/Design-System?node-id=0%3A1&mode=dev">Ivy design system</a>.</small>
    <br/>
    <br/>
</div>

<div style="text-align: center">
    <img src="https://avatars.githubusercontent.com/u/86833171?v=4" width="100px;" alt="Aditya [ADX]"/><br>
    <strong><a href="https://github.com/adx69" >Aditya</a> </strong><br>
    <br/>
</div>

<div style="text-align: center">
    <img src="https://avatars.githubusercontent.com/u/130169485?v=4" width="100px;" alt="Shymom [SSI]"/><br>
    <strong><a href="https://github.com/SHYMOM" >Shymom</a> </strong><br>
    <br/>
</div>
