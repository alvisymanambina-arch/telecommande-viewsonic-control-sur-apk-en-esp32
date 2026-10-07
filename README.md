# Télécommande ViewSonic — ESP32 et Android

> Télécommande infrarouge pour écran ViewSonic, pilotée par une application Android via Wi-Fi et un ESP32.

Le projet transforme un ESP32 en passerelle Wi-Fi vers infrarouge. L'application Android envoie une commande HTTP au microcontrôleur, qui transmet ensuite le code IR RC5 correspondant à l'écran ViewSonic.

## Fonctionnalités

- Point d'accès Wi-Fi créé par l'ESP32.
- API HTTP simple : `/cmd?val=COMMANDE`.
- Émission infrarouge RC5 depuis le GPIO 4.
- Commandes de mise sous tension, navigation, volume, source, luminosité, capture, gel d'écran et ratio.
- Application Android de contrôle avec boutons de télécommande.

## Architecture

```text
[ Application Android ] -- Wi-Fi / HTTP --> [ ESP32 ] -- IR RC5 --> [ Écran ViewSonic ]
                                             |
                                             `-- GPIO 4 -> transistor -> LED infrarouge
```

## Contenu

| Dossier / fichier | Description |
| --- | --- |
| `esp32_viewsonic_remote/` | Firmware Arduino pour ESP32 et émission IR. |
| `Telecom/` | Application Android Studio (Java). |
| `Telecomande.txt` | Référence des codes et commandes infrarouges. |

## Matériel requis

- ESP32
- LED infrarouge
- Transistor NPN (2N2222 ou BC547)
- Résistance de base : 220 Ω ou 330 Ω
- Résistance pour LED IR : 10 Ω ou 22 Ω
- Écran ViewSonic compatible avec les codes RC5 définis dans le firmware
- Smartphone Android connecté au point d'accès de l'ESP32

## Câblage IR

Le firmware utilise le **GPIO 4** pour l'émission infrarouge. Utilisez un transistor pour alimenter correctement la LED IR :

- GPIO 4 → résistance de base → base du transistor
- 5 V/Vin → collecteur du transistor
- Émetteur → anode de la LED IR
- Cathode de la LED IR → résistance → GND

## Démarrage

### 1. ESP32

1. Ouvrir `esp32_viewsonic_remote/esp32_viewsonic_remote.ino` dans l'IDE Arduino.
2. Installer la carte ESP32 et la bibliothèque `IRremoteESP8266`.
3. Téléverser le programme sur l'ESP32.
4. Connecter le téléphone au point d'accès Wi-Fi créé par l'ESP32.

> Avant le déploiement, modifiez le nom et le mot de passe du point d'accès dans le firmware.

### 2. Application Android

1. Ouvrir le dossier `Telecom/` dans Android Studio.
2. Laisser Gradle synchroniser le projet.
3. Construire et installer l'application sur un appareil Android (minSdk 24, targetSdk 36).
4. Connecter le téléphone au Wi-Fi de l'ESP32, puis utiliser les boutons de l'application.

## Commandes HTTP

Exemple :

```text
http://192.168.4.1/cmd?val=POWER
```

Commandes prises en charge :

`POWER`, `WINDOWS`, `PENCIL`, `HOME`, `BACK`, `VOL_UP`, `VOL_DOWN`, `MUTE`, `INFO`, `SOURCE`, `MENU_PC`, `EXIT_PC`, `SETTINGS`, `OK`, `UP`, `DOWN`, `LEFT`, `RIGHT`, `BR_UP`, `BR_DOWN`, `BLANK`, `CAPTURE`, `FREEZE`, `ASPECT`, `0` à `9`, `RED`, `GREEN`, `YELLOW`, `BLUE`.

## Dépendances

- Arduino ESP32
- `WiFi.h`
- `WebServer.h`
- `IRremoteESP8266`
- Android Studio / Gradle
