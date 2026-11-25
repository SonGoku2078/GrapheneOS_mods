# Launcher Plus Mini

Eigenständige Android-Start-App mit Paket-ID `io.github.graphenelauncherplus.mini`.
Die App verwendet Java und öffentliche Android-APIs. Der Build benötigt JDK und Android-SDK
(API 36, Build Tools 36.0.0). Der Launcher verlangt keine Android-Berechtigungen und enthält
keine Drittanbieter-Bibliotheken. Die Beschriftungen werden schwarz auf hellem Hintergrund
gezeichnet. Auf dem Smartphone wurde nichts installiert.

## Bedienung

- **Alle Apps** antippen oder vom unteren Bereich des Startbildschirms nach oben wischen.
- Im Drawer eine App gedrückt halten und den Startbildschirm oder einen Ordner wählen.
- Auf dem Startbildschirm mit **Ordner** einen Ordner anlegen. Antippen öffnet ihn.
  Gedrückt halten öffnet das Menü zum Umbenennen oder Entfernen.
- Über **Widgets** einen Anbieter wählen und gegebenenfalls Androids Bind-Dialog bestätigen.
- Androids Einstellungen für die Standard-Start-App erlauben den Wechsel zwischen den Launchern.

## Bekannte Einschränkungen

- Eigenes Layout mit einer hellen Oberfläche. Schwarz ist fest eingestellt.
- Ein Startbildschirm mit scrollbarem Inhalt. Apps und Ordner werden über Menüs verwaltet.
  Freie Platzierung per Drag-and-drop und frei skalierbare Widgets sind offen.
- Widget-Inhalte stammen vom Anbieter und verwenden dessen Gestaltung und Funktionen.
- Der GitHub-Workflow signiert mit einem pro Lauf erzeugten Testschlüssel. Builds aus
  verschiedenen Läufen können sich daher bei einem Update gegenseitig nicht ersetzen.
- Emulatorläufe testen die öffentliche Android-Schnittstelle. Kompatibilität mit einem
  physischen GrapheneOS-Gerät und Arbeitsprofilen steht aus.

## Prüfung

Der erste Actions-Lauf baute und signierte die APK, installierte sie und fand beide HOME-Handler.
Der UI-Test scheiterte beim ersten Zugriff auf den Android-UI-Baum. [Lauf 2](https://github.com/SonGoku2078/GrapheneOS_mods/actions/runs/45634433186)

| Durchlauf | Beobachtung und Korrektur |
| --- | --- |
| [4](https://github.com/SonGoku2078/GrapheneOS_mods/actions/runs/45635138710), [6](https://github.com/SonGoku2078/GrapheneOS_mods/actions/runs/45635301721) | Start-App sichtbar. Der Test adressierte Androids Dialogschaltfläche anschliessend über ihre Ressourcen-ID. |
| [8](https://github.com/SonGoku2078/GrapheneOS_mods/actions/runs/45635818079), [10](https://github.com/SonGoku2078/GrapheneOS_mods/actions/runs/45636559076) | Ordner angelegt. Die Screenshot-Prüfung wurde auf PNG umgestellt und bestätigte schwarze Pixel in App- und Ordnernamen. |
| [12](https://github.com/SonGoku2078/GrapheneOS_mods/actions/runs/45637236689), [14](https://github.com/SonGoku2078/GrapheneOS_mods/actions/runs/45638007457) | Androids moderner Zurück-Callback wurde ergänzt. Der Test trennte danach den Schliessvorgang des Anheftdialogs von der Zurück-Prüfung. App-Start aus dem Ordner erreicht. |
| [16](https://github.com/SonGoku2078/GrapheneOS_mods/actions/runs/45638790706) | Der Scrolltest erreichte den Widget-Anbieter und bestätigte die Systembindung. Die Widget-Prüfung wurde anschliessend auf den gerenderten Widget-Bereich umgestellt. |
| [18](https://github.com/SonGoku2078/GrapheneOS_mods/actions/runs/45639701392) | **Bestanden.** APK gebaut, signiert und ohne deklarierte Berechtigungen im Android-16-Emulator installiert. Separaten HOME-Handler ausgewählt und den vorhandenen Handler weiterhin gefunden. Ordner angelegt, geöffnet und nach Neustart wiedergefunden. App-Drawer per Wischgeste geöffnet, Android-Einstellungen über einen Ordner gestartet und Zurück-Navigation geprüft. Schwarze Glyphen in App- und Ordnernamen im Screenshot bestätigt. Test-Widget über den Android-Systemdialog gebunden, Pixel des gerenderten Widgets geprüft und Anzeige nach Neustart sowie Entfernen bestätigt. |

[APK, Screenshots und Berechtigungsprüfung als ZIP](https://github.com/SonGoku2078/GrapheneOS_mods/actions/runs/45639701392/artifacts/15035770465)
