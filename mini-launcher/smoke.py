"""Exercise Launcher Plus Mini through Android's UI on a disposable emulator."""
import re
import io
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

from PIL import Image

PACKAGE = "io.github.graphenelauncherplus.mini"
COMPONENT = f"{PACKAGE}/.HomeActivity"


def adb(*args):
    return subprocess.check_output(["adb", "-e", *args], timeout=30)


def snapshot():
    path = "/data/local/tmp/launcher-test.xml"
    for _ in range(5):
        adb("shell", "rm", "-f", path)
        result = subprocess.run(
            ["adb", "-e", "shell", "uiautomator", "dump", path],
            capture_output=True, timeout=20, check=False)
        if result.returncode == 0 and b"dumped to" in result.stdout:
            tree = ET.fromstring(adb("exec-out", "cat", path))
            Path("mini-last-ui.xml").write_text(ET.tostring(tree, encoding="unicode"))
            return tree
        time.sleep(1)
    screenshot("ui-unavailable")
    print(adb("shell", "dumpsys", "activity", "activities").decode()[-4000:], flush=True)
    print(adb("logcat", "-d", "-s", "AndroidRuntime").decode()[-4000:], flush=True)
    raise AssertionError("No accessible Android UI after five attempts")


def node(text):
    for _ in range(4):
        tree = snapshot()
        for item in tree.iter("node"):
            if any(item.get(key, "").casefold() == text.casefold()
                   for key in ("text", "content-desc", "resource-id")):
                return item
        time.sleep(0.5)
    screenshot("missing-element")
    visible = [(item.get("text"), item.get("content-desc")) for item in tree.iter("node")
               if item.get("text") or item.get("content-desc")]
    raise AssertionError(f"Missing {text!r}. Visible: {visible}")


def bounds(item):
    return tuple(map(int, re.findall(r"\d+", item.get("bounds"))))


def tap(text, long=False):
    left, top, right, bottom = bounds(node(text))
    x, y = str((left + right) // 2), str((top + bottom) // 2)
    if long:
        adb("shell", "input", "swipe", x, y, x, y, "750")
    else:
        adb("shell", "input", "tap", x, y)


def choose_widget(text):
    for _ in range(8):
        tree = snapshot()
        if any(item.get("text") == text for item in tree.iter("node")):
            tap(text)
            return
        lists = [item for item in tree.iter("node") if item.get("scrollable") == "true"]
        if not lists:
            break
        left, top, right, bottom = bounds(lists[0])
        x = str((left + right) // 2)
        adb("shell", "input", "swipe", x, str(top + (bottom - top) * 4 // 5),
            x, str(top + (bottom - top) // 4), "450")
    screenshot("widget-provider-unavailable")
    raise AssertionError(f"Widget provider missing from the picker: {text!r}")


def capture_label(text):
    labels = [item for item in snapshot().iter("node") if item.get("text") == text]
    assert labels, f"No text node for {text!r}"
    left, top, right, bottom = bounds(labels[0])
    with Image.open(io.BytesIO(adb("exec-out", "screencap", "-p"))) as screenshot_image:
        display_width, display_height = map(int, re.findall(
            r"([0-9]+)x([0-9]+)", adb("shell", "wm", "size").decode())[-1])
        box = (
            round(left * screenshot_image.width / display_width),
            round(top * screenshot_image.height / display_height),
            round(right * screenshot_image.width / display_width),
            round(bottom * screenshot_image.height / display_height),
        )
        crop = screenshot_image.crop(box).convert("RGB")
        black = sum(pixel == (0, 0, 0) for pixel in crop.getdata())
        assert black >= 3, f"No black glyph pixels found for {text!r}"


def screenshot(name):
    Path(f"mini-{name}.png").write_bytes(adb("exec-out", "screencap", "-p"))


adb("shell", "settings", "put", "global", "device_provisioned", "1")
adb("shell", "settings", "put", "secure", "user_setup_complete", "1")
adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
adb("shell", "wm", "dismiss-keyguard")
adb("shell", "wm", "size", "720x1280")
adb("shell", "wm", "density", "240")
adb("install", "mini-launcher/build/LauncherPlusMini.apk")
adb("install", "mini-launcher/build/test-widget.apk")
homes = adb("shell", "cmd", "package", "query-activities", "--brief", "-a",
            "android.intent.action.MAIN", "-c", "android.intent.category.HOME").decode()
assert PACKAGE in homes, homes
assert any("/" in line and PACKAGE not in line for line in homes.splitlines()), homes
selected = adb("shell", "cmd", "package", "set-home-activity", COMPONENT).decode()
assert "Success" in selected, selected
started = adb("shell", "am", "start", "-W", "-n", COMPONENT).decode()
assert "Status: ok" in started, started
node("Alle Apps")
tap("Ordner")
node("Neuer Ordner")
tap("Ordnername")
adb("shell", "input", "text", "Testordner")
tap("android:id/button1")
capture_label("Testordner")
tap("Testordner")
node("Umbenennen")
screenshot("folder")
adb("shell", "input", "keyevent", "4")
left, top, right, bottom = bounds(snapshot().find("node"))
x = str((left + right) // 2)
adb("shell", "input", "swipe", x, str(top + int((bottom - top) * 0.84)),
    x, str(top + int((bottom - top) * 0.42)), "400")
node("Start")
drawer = snapshot()
assert any(item.get("clickable") == "true" and item.get("content-desc")
           for item in drawer.iter("node")), "Drawer has no app tiles"
capture_label("Settings")
screenshot("drawer")
tap("Settings", long=True)
tap("Ordner: Testordner")
tap("Start")
tap("Testordner")
node("Settings")
tap("Settings")
for _ in range(10):
    resumed = adb("shell", "dumpsys", "activity", "activities").decode()
    if any("ResumedActivity" in line and "com.android.settings/" in line
           for line in resumed.splitlines()):
        break
    time.sleep(0.5)
else:
    raise AssertionError("Settings never reached the foreground")
adb("shell", "input", "keyevent", "3")
node("Testordner")
tap("Widgets")
node("Widget hinzufügen")
choose_widget("Launcher Test Widget")
tap("android:id/button1")
node("Widget Testinhalt")
screenshot("widget")
adb("shell", "am", "force-stop", PACKAGE)
adb("shell", "am", "start", "-n", COMPONENT)
node("Testordner")
node("Widget Testinhalt")
screenshot("home")
tap("Launcher Test Widget entfernen")
tap("android:id/button1")
assert not any(item.get("text") == "Widget Testinhalt" for item in snapshot().iter("node"))
tap("Alle Apps")
node("Settings")
adb("shell", "input", "keyevent", "4")
node("Testordner")
print("PASS: install, HOME selection and coexistence, drawer swipe, app launch, "
      "folder creation/pinning/persistence, black glyphs, widget binding, display, persistence and removal.")
