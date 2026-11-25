# Graphene Launcher Plus – Build-Analyse und Zwischenbericht

## Build und Systemintegration

- Upstream-Basis für den Patch ist Branch `17` von [GrapheneOS Launcher3](https://github.com/GrapheneOS/platform_packages_apps_Launcher3/tree/17). Der Workspace enthielt beim Start keinen vollständigen Checkout.
- Der [Soong-Build](https://github.com/GrapheneOS/platform_packages_apps_Launcher3/blob/17/Android.bp) hat ein Ziel `Launcher3` mit `src_no_quickstep` und öffentlichen SDK-Stubs. Dieses Ziel ist trotzdem als privilegierte System-App definiert. Der Build nutzt unter anderem AndroidX, SystemUI-Bibliotheken, Dagger, generierten BuildConfig-Code und Protobuf. Ein Standalone-Build muss diese Abhängigkeiten auflösen und privilegierte Laufzeitpfade prüfen.
- Quickstep verwendet Plattform-APIs und beantragt beispielsweise `MANAGE_ACTIVITY_TASKS` und `MONITOR_INPUT`. Für die separat installierbare App ist das Ziel ohne Quickstep der Ausgangspunkt. Die systemverwaltete Übersicht und Gestennavigation müssen im Emulator separat von Launcher-Gesten beobachtet werden. [Quickstep-Manifest](https://github.com/GrapheneOS/platform_packages_apps_Launcher3/blob/17/quickstep/AndroidManifest.xml)
- Vorgesehene App-ID ist `io.github.graphenelauncherplus`. Beim Port sind alle zusammengeführten Provider-Autoritäten, benutzerdefinierten Berechtigungen, Paketplatzhalter und hartcodierten Paketverweise zu prüfen. Die App soll mit einem eigenen Schlüssel signiert und als reguläre HOME-App installiert werden. [Gemeinsames Manifest](https://github.com/GrapheneOS/platform_packages_apps_Launcher3/blob/17/AndroidManifest-common.xml)
- Das gemeinsame Manifest beantragt unter anderem `BIND_APPWIDGET` und `READ_DEVICE_CONFIG`. Der vorhandene Widget-Pfad versucht `bindAppWidgetIdIfAllowed` und öffnet bei Rückgabewert `false` den Systemdialog `ACTION_APPWIDGET_BIND`. Der Launcher behandelt dessen Zustimmung und Abbruch bereits im Quellcode. Im Standalone-Build muss geprüft werden, ob dieser Pfad ohne Systemberechtigungen erreichbar ist und sich ein Widget anschliessend platzieren lässt. [Launcher.java](https://github.com/GrapheneOS/platform_packages_apps_Launcher3/blob/17/src/com/android/launcher3/Launcher.java) · [LauncherWidgetHolder.java](https://github.com/GrapheneOS/platform_packages_apps_Launcher3/blob/17/src/com/android/launcher3/widget/LauncherWidgetHolder.java)
- Im finalen APK sind die zusammengeführten Berechtigungen und Abhängigkeiten auf Internetzugriff, privilegierte Berechtigungen und zusätzliche Tracker zu prüfen. Die Upstream-Untergrenze für das Ziel ohne Quickstep ist API 31. [Soong-Build](https://github.com/GrapheneOS/platform_packages_apps_Launcher3/blob/17/Android.bp)

## Patch und Verifikation

`black-labels.patch` setzt App-Beschriftungen im Workspace, in Ordnern und im App-Drawer auf Schwarz. Er erfasst zusätzlich bestehende App-Ansichten für Vorschläge und Suche. Geschlossene Ordner nutzen dieselbe Beschriftungsansicht wie der Workspace. Der Titel im geöffneten Ordner erhält die Farbe direkt. Die Anpassung lässt den bestehenden Alpha-Pfad für Animationen bestehen. Schwarz, Weiss und Automatisch sind als spätere Einstellung vorgesehen. Schwarze Beschriftungen können auf dunklen Hintergründen schlecht lesbar sein.

| Prüfung | Status |
| --- | --- |
| Patch auf den beiden bezogenen Originaldateien prüfen, anwenden und gegen die bearbeiteten Dateien vergleichen | Bestanden |
| Prüfen, ob sich der Patch vollständig rückwärts anwenden lässt | Bestanden |
| Kompilieren, signieren, Paket-ID, Manifest und Abhängigkeiten des APK prüfen | Ausstehend |
| Koexistenz mit dem vorinstallierten Launcher und Wechsel der HOME-App | Ausstehend |
| Ordner erstellen, öffnen, umbenennen und nach Launcher-Neustart prüfen | Ausstehend |
| Widget-Auswahl, Bind-Dialog, Platzieren und Grössenänderung | Ausstehend |
| App-Drawer, App-Start und Launcher-Gesten | Ausstehend |
| HOME- und Zurück-Geste sowie Systemübersicht | Ausstehend |
| Schwarze Schrift visuell bei hellem und dunklem Hintergrund prüfen | Ausstehend |
| Drei vollständige Review-, Test- und Korrekturdurchläufe an der APK | Ausstehend |

## Blocker

Der direkte Git-Zugriff scheiterte an der Namensauflösung. Der GitHub-Connector lieferte einzelne Quelldateien für die Analyse und Patch-Prüfung. Lokal war Java verfügbar. Android-SDK, Gradle, ADB und Emulator wurden bei der Werkzeugprüfung nicht gefunden. `/dev/kvm` war ebenfalls nicht verfügbar. Für die Laufzeittests ist daher eine Emulator-Umgebung mit funktionierender Beschleunigung oder ein nutzbarer Software-Emulator erforderlich.

Es wurde keine APK gebaut und kein Smartphone verwendet.

Nach der Freigabe der grossen Downloads wurden die Abrufwege erneut getestet. GitHub, GitHub Codeload, Google Android SDK, Gradle und Maven Central scheiterten in der Shell bei der Namensauflösung. Ein konfigurierter HTTP-Proxy, ein Cloud-Pakethelfer und eine vorhandene SDK-Installation wurden nicht gefunden. Die Downloadfreigabe liegt vor. Für den nächsten Build-Schritt braucht es ausgehenden Netzwerkzugriff aus der Build-Umgebung oder einen vollständigen lokalen Checkout samt Offline-Toolchain und Abhängigkeiten.

## GitHub Actions und Soong-Ziel

Die Arbeit läuft im [Entwurfs-PR](https://github.com/SonGoku2078/GrapheneOS_mods/pull/1). Der erste [Actions-Lauf](https://github.com/SonGoku2078/GrapheneOS_mods/actions/runs/45630751878) hat den Schwarz-Patch auf einem vollständigen Launcher3-Checkout angewandt und den Diff geprüft. Der Emulator-Job scheiterte, weil `sdkmanager` auf dem Runner nicht im Suchpfad lag. Die korrigierte Konfiguration leitet die Pfade aus der SDK-Installation ab. Im [zweiten Lauf](https://github.com/SonGoku2078/GrapheneOS_mods/actions/runs/45630946255) wurden die SDK-Pakete installiert. ADB meldete innerhalb von 180 Sekunden keinen Emulator. Im [Lauf mit gepinntem Upstream](https://github.com/SonGoku2078/GrapheneOS_mods/actions/runs/45631224512) bestanden beide Patches die Quellprüfung. Das [Startprotokoll](https://github.com/SonGoku2078/GrapheneOS_mods/actions/runs/45631954924/job/139267232146) wies anschliessend ein fehlendes AVD im Suchverzeichnis des Emulators nach. Der Workflow setzt dafür ein gemeinsames Verzeichnis für beide Android-Werkzeuge. Der [abschliessende Vorabcheck](https://github.com/SonGoku2078/GrapheneOS_mods/actions/runs/45632282265) bestand beide Quellprüfungen und meldete einen vollständig gestarteten Android-36-Emulator. Dabei wurde keine Launcher-APK installiert.

`standalone-soong.patch` ergänzt das separate Ziel `GrapheneLauncherPlus` auf dem Launcher3-Stand `0ca6a5b36620dd6a4f83b4162ef006889184e19e`. Es verwendet den Quellpfad ohne Quickstep, eigene Manifestdateien und dieselbe App-ID im generierten `BuildConfig` und in den Provider-Autoritäten. Bei der lokalen Prüfung liess sich der Patch auf die zum Commit gehörigen Dateien anwenden und rückgängig machen. Die beiden neuen Manifestdateien wurden als XML geparst und ihre Provider-Autoritäten und bekannten privilegierten Berechtigungen geprüft. Kompilierung und der zusammengeführte APK-Manifeststand stehen aus.

Der Soong-Build setzt einen kompatiblen GrapheneOS-17-Plattformcheckout voraus. Die offizielle Anleitung nennt bereits für den leichtgewichtigen Sync mindestens 90 GiB Speicher. GitHubs Standard-Runner bietet 14 GB SSD-Speicher. Der Standard-Runner dient den Quellprüfungen und dem Emulator. Für die Soong-Kompilierung fehlt ein Runner mit Plattformcheckout und ausreichend Speicher. [GrapheneOS-Build-Anleitung](https://grapheneos.org/build) · [GitHub-Runner-Spezifikationen](https://docs.github.com/en/actions/reference/runners/github-hosted-runners)

Zum Kompilieren in einem separaten, bereits synchronisierten GrapheneOS-17-Baum sind der gepinnte Launcher3-Stand und die beiden Patches erforderlich. Die folgende Befehlsfolge ist vorbereitet und wurde hier noch nicht ausgeführt.

```bash
git -C packages/apps/Launcher3 apply --check \
  /pfad/zu/GrapheneOS_mods/black-labels.patch \
  /pfad/zu/GrapheneOS_mods/standalone-soong.patch
git -C packages/apps/Launcher3 apply \
  /pfad/zu/GrapheneOS_mods/black-labels.patch \
  /pfad/zu/GrapheneOS_mods/standalone-soong.patch
source build/envsetup.sh
lunch sdk_phone64_x86_64-cur-userdebug
m GrapheneLauncherPlus
```

Vor der APK-Freigabe sind eine eigene Signatur, eine Prüfung des binären Manifests und der Abhängigkeiten sowie die Installation als reguläre App im Emulator erforderlich.
