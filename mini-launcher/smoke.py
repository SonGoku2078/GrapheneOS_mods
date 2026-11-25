"""Exercise Launcher Plus Mini through Android's UI on a disposable emulator."""
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = "io.github.graphenelauncherplus.mini"
COMPONENT = f"{PACKAGE}/.HomeActivity"


def adb(*args):
    return subprocess.check_output(["adb", "-e", *args], timeout=30)


def snapshot():
    adb("shell", "uiautomator", "dump", "/sdcard/launcher-test.xml")
    return ET.fromstring(adb("exec-out", "cat", "/sdcard/launcher-test.xml"))


def node(text):
    for _ in range(4):
        for item in snapshot().iter("node"):
            if item.get("text") == text or item.get("content-desc") == text:
                return item
        time.sleep(0.5)
    raise AssertionError(f"Missing UI element {text!r}")


def tap(text):
    left, top, right, bottom = map(int, re.findall(r"\d+", node(text).get("bounds")))
    adb("shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2))


def screenshot(name):
    Path(f"mini-{name}.png").write_bytes(adb("exec-out", "screencap", "-p"))


adb("shell", "input", "keyevent", "82")
adb("install", "mini-launcher/build/LauncherPlusMini.apk")
homes = adb("shell", "cmd", "package", "query-activities", "--brief", "-a",
            "android.intent.action.MAIN", "-c", "android.intent.category.HOME").decode()
assert PACKAGE in homes, homes
assert any("/" in line and PACKAGE not in line for line in homes.splitlines()), homes
adb("shell", "am", "start", "-n", COMPONENT)
node("Alle Apps")
tap("Ordner")
node("Neuer Ordner")
adb("shell", "input", "text", "Testordner")
tap("Speichern")
tap("Testordner")
node("Umbenennen")
screenshot("folder")
adb("shell", "input", "keyevent", "4")
tap("Alle Apps")
drawer = snapshot()
assert any(item.get("clickable") == "true" and item.get("content-desc")
           for item in drawer.iter("node")), "Drawer has no app tiles"
screenshot("drawer")
adb("shell", "input", "keyevent", "4")
tap("Widgets")
node("Widget hinzufügen")
adb("shell", "input", "keyevent", "4")
adb("shell", "am", "force-stop", PACKAGE)
adb("shell", "am", "start", "-n", COMPONENT)
node("Testordner")
screenshot("home")
print("PASS: APK install, two HOME handlers, folder creation/open/persistence, app drawer, widget picker.")
