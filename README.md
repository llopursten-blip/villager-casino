# Villager Casino (Fabric, Minecraft 26.3)

Ein neuer **Berufsblock** für Dorfbewohner. Sobald ein Dorfbewohner ihn als Arbeitsplatz annimmt,
**verwandelt sich der Block in einen Spielautomaten**, an dem Spieler mit Smaragden spielen können.

## So funktioniert es im Spiel

1. **Casino-Tisch** craften (Kreativ-Tab "Funktionale Blöcke"):
   ```
   G E G      G = Goldbarren
   I R I      E = Smaragd
   I I I      I = Eisenbarren, R = Redstone
   ```
2. Block neben einen **arbeitslosen Dorfbewohner** stellen (kein Nitwit, kein Beruf mit XP).
3. Der Dorfbewohner läuft hin, wird zum **Croupier** (grüne Weste) – und der Block wird zum **Spielautomaten**
   (Funken, Sound, leuchtet leicht, Vorderseite zeigt zum Spieler).
4. **Rechtsklick mit einem Smaragd** in der Hand = 1 Dreh:

| Ergebnis            | Auszahlung (Smaragde) |
|---------------------|-----------------------|
| ♥ ♥ ♥               | 4                     |
| ♣ ♣ ♣               | 6                     |
| ♦ ♦ ♦               | 10                    |
| ★ ★ ★               | 20                    |
| 7 7 7 (Jackpot)     | 64                    |
| zwei gleiche        | Einsatz zurück (1)    |
| sonst               | verloren              |

Erwartungswert ca. 0,92 Smaragde pro Smaragd (rund 8 % Hausvorteil). Werte und Gewichtungen stehen in
`SlotSymbol.java`.

Der Croupier handelt außerdem (Level 1–5): Goldbarren→Smaragd, Kekse, Uhr, Goldapfel, Diamant, Totem.
Die Trades liegen in `data/villagercasino/villager_trade/` und `trade_set/` und lassen sich ohne Code ändern.

## Bauen

Voraussetzungen: **JDK 25**, Gradle **9.6.0**, Internetzugang für den ersten Build.

```bash
gradle wrapper --gradle-version 9.6.0   # einmalig, erzeugt gradlew
./gradlew build                          # Mod-JAR liegt danach in build/libs/
./gradlew runClient                      # Testen im Entwicklungs-Client
```

Alternativ: Projekt über https://fabricmc.net/develop/template/ für 26.3 erzeugen und nur den Ordner `src/`
sowie die Werte in `gradle.properties` übernehmen.

Spieler brauchen **Fabric Loader ≥ 0.19.5** und **Fabric API 0.165.0+26.3** (oder neuer).
Die Mod muss auf **Server und Client** installiert sein (neuer Block und Beruf).

## Wie die Verwandlung technisch gelöst ist

* Es gibt nur **einen Block** (`casino_block`) mit der Eigenschaft `slot_machine` (false/true) und `facing`.
* Beide Zustände gehören zum **selben POI-Typ** (`ModProfessions`). Würde der Block durch einen *anderen* Block
  ersetzt, verlöre der Dorfbewohner seinen Job – so bleibt er stabil angestellt.
* Ein freier Block prüft per geplantem Tick (jede Sekunde), ob ein Dorfbewohner in der Nähe ihn als
  `JOB_SITE` gespeichert hat (`CasinoBlock#isClaimedByVillager`). Wenn ja → `slot_machine=true`.
* Kein Mixin nötig.

## Nicht getestet – hier könnte es klemmen

Dieses Projekt wurde **ohne Compiler- und Spieltest** geschrieben (kein Netzwerk in der Erstellungsumgebung).
Die APIs wurden gegen die Fabric-Ankündigung für 26.3 und die NeoForge-Primer (1.21.11→26.1, 26.2→26.3)
abgeglichen. Falls der Build oder der Start Fehler meldet, sind das die wahrscheinlichsten Stellen:

1. `PoiTypes.register(...)` – in Vanilla `private`, per Fabric-API-Access-Widener öffentlich (ab 0.163 für 26.3).
2. Konstruktor von `VillagerProfession` (7 Parameter inkl. Trade-Set-Map) – Stand 26.1-Primer.
3. Felder in `villager_trade/*.json` und `trade_set/*.json` (Zahlenanbieter wurden in 26.3 umgebaut;
   einfache Konstanten sollten weiter gehen).
4. Pfad der Berufs-Textur `textures/entity/villager/profession/croupier.png` – sonst erscheint die
   Lila-Schwarz-Textur auf dem Dorfbewohner.
5. Kleinere Methodennamen wie `displayClientMessage`, `useWithoutItem`, `popResourceFromFace`.

Schick mir die Fehlermeldung, dann passe ich es an.
