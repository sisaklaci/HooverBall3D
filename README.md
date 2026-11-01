# HooverBall 3D Soccer

3D hooverball-foci FPS nézetben, libGDX-szel, közös Java kódbázissal Windowsra és Androidra.

## Modulok

- `core` – a teljes játéklogika, fizika, AI és renderelés (platformfüggetlen).
- `desktop` – Windows/Linux/Mac natív indító (LWJGL3).
- `android` – Android indító. Csak akkor épül be, ha van beállított Android SDK-d (lásd lent).

## Futtatás Windows/desktop alatt

```
gradlew.bat :desktop:run
```

(Ha nincs `gradlew.bat`-od, töltsd le a Gradle wrappert: `gradle wrapper --gradle-version 8.9` egy telepített Gradle-lel, vagy nyisd meg a projektet IntelliJ IDEA-ban, ami ezt automatikusan megteszi.)

## Futtatás Androidon

1. Telepítsd az Android SDK-t (pl. Android Studio-val).
2. Hozz létre egy `local.properties` fájlt a projekt gyökerében:
   ```
   sdk.dir=C\:\\Users\\<neved>\\AppData\\Local\\Android\\Sdk
   ```
3. Nyisd meg a projektet Android Studio-ban – az `android` modul automatikusan bekerül a buildbe, mivel a `local.properties` már létezik.
4. Futtasd a `android` konfigurációt egy csatlakoztatott eszközön vagy emulátoron.

## Irányítás

**Windows (egér + billentyűzet):**
- `W / A / S / D` vagy nyílbillentyűk – mozgás
- Egér – körülnézés
- Bal/jobb nyíl – kanyarodás (egér nélkül is irányítható)
- `Shift` vagy `Space` (tartva) – booster (nagyobb sebesség, amíg van energia)
- `ESC` – szünet / folytatás

**Android (érintés):**
- Bal oldali virtuális joystick – mozgás
- Jobb oldali húzás – körülnézés
- Jobb alsó gomb (tartva) – booster
- Jobb felső gomb / vissza gomb – szünet

## Játékmódok

A főmenüben választható:
- **Pontszámra megy** – az első csapat, aki eléri a kiválasztott gólszámot (3/5/10), nyer.
- **Időre megy** – a kiválasztott időkorlát (2/5/10 perc) lejártakor a több góllal álló csapat nyer; döntetlen is lehetséges.

A pálya szélei, valamint a hooverball és mindkét jármű is visszapattannak a pálya határairól. A sarkok lekerekítettek, így sem a labda, sem a gépjármű nem tud beragadni egy sarokba.

## Készítette

[helmetsoft.com](https://helmetsoft.com) – ez a játékon belül a főmenü jobb alsó sarkában is fel van tüntetve.

## Licenc

A projekt a [PolyForm Noncommercial 1.0.0](LICENSE) licenc alatt áll: szabadon
nézhető, módosítható és terjeszthető, de csak nem kereskedelmi célra.
