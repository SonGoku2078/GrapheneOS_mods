package io.github.graphenelauncherplus.mini;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class HomeActivity extends Activity {
    private static final int BIND_WIDGET = 1;
    private static final int CONFIGURE_WIDGET = 2;
    private static final int BACKGROUND = 0xfff5f3ef;
    private static final int TILE = 0xffffffff;

    private LinearLayout root;
    private JSONArray home;
    private AppWidgetHost widgetHost;
    private AppWidgetManager widgetManager;
    private int pendingWidget = -1;
    private boolean drawer;
    private int folderIndex = -1;
    private float touchX;
    private float touchY;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        widgetManager = AppWidgetManager.getInstance(this);
        widgetHost = new AppWidgetHost(this, 1985);
        try {
            home = new JSONArray(getPreferences(0).getString("home", "[]"));
        } catch (JSONException invalidState) {
            home = new JSONArray();
        }
        if (state != null) {
            pendingWidget = state.getInt("widget", -1);
        }
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
        }
        render();
    }

    @Override protected void onStart() {
        super.onStart();
        widgetHost.startListening();
    }

    @Override protected void onStop() {
        widgetHost.stopListening();
        super.onStop();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("widget", pendingWidget);
        super.onSaveInstanceState(state);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        showHome();
    }

    @Override public void onBackPressed() {
        handleBack();
    }

    private void handleBack() {
        if (drawer || folderIndex >= 0) {
            showHome();
        }
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            touchX = event.getX();
            touchY = event.getY();
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP && !drawer
                && folderIndex < 0 && touchY > root.getHeight() * 0.75f
                && touchY - event.getY() > dp(70)
                && Math.abs(touchX - event.getX()) < dp(70)) {
            MotionEvent cancel = MotionEvent.obtain(event);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            super.dispatchTouchEvent(cancel);
            cancel.recycle();
            showDrawer();
            return true;
        }
        return super.dispatchTouchEvent(event);
    }

    private void showHome() {
        drawer = false;
        folderIndex = -1;
        render();
    }

    private void showDrawer() {
        drawer = true;
        folderIndex = -1;
        render();
    }

    private void render() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            view.setPadding(dp(16) + bars.left, dp(12) + bars.top,
                    dp(16) + bars.right, dp(12) + bars.bottom);
            return insets;
        });
        setContentView(root);
        root.requestApplyInsets();

        JSONObject folder = folderIndex < 0 ? null : home.optJSONObject(folderIndex);
        String title = drawer ? "Alle Apps" : folder == null ? "Start" : folder.optString("name");
        TextView heading = label(title, 25);
        heading.setPadding(dp(4), 0, 0, dp(12));
        root.addView(heading);

        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        content.addView(grid);

        if (drawer) {
            for (ResolveInfo app : apps()) {
                ComponentName component = component(app);
                addAppTile(grid, app.activityInfo, app.loadLabel(getPackageManager()),
                        () -> launch(component), () -> pinMenu(component));
            }
            root.addView(button("Start", this::showHome));
        } else if (folder != null) {
            JSONArray entries = folder.optJSONArray("apps");
            for (int i = 0; i < entries.length(); i++) {
                int item = i;
                addStoredApp(grid, entries.optString(i), () -> {
                    entries.remove(item);
                    saveAndRender();
                });
            }
            LinearLayout actions = actions();
            actions.addView(button("Umbenennen", () -> folderName(folder)));
            actions.addView(button("Start", this::showHome));
        } else {
            for (int i = 0; i < home.length(); i++) {
                JSONObject entry = home.optJSONObject(i);
                int index = i;
                if ("widget".equals(entry.optString("kind"))) {
                    addWidgetView(content, entry.optInt("id"));
                } else if ("folder".equals(entry.optString("kind"))) {
                    addTile(grid, entry.optString("name"), null, () -> {
                        folderIndex = index;
                        render();
                    }, () -> homeMenu(index));
                } else {
                    addStoredApp(grid, entry.optString("component"), () -> {
                        home.remove(index);
                        saveAndRender();
                    });
                }
            }
            LinearLayout actions = actions();
            actions.addView(button("Ordner", () -> folderName(null)));
            actions.addView(button("Widgets", this::widgetMenu));
            root.addView(button("Alle Apps", this::showDrawer));
        }
    }

    private LinearLayout actions() {
        LinearLayout row = new LinearLayout(this);
        root.addView(row);
        return row;
    }

    private List<ResolveInfo> apps() {
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager manager = getPackageManager();
        List<ResolveInfo> result = new ArrayList<>(manager.queryIntentActivities(query, 0));
        result.sort(Comparator.comparing(app -> app.loadLabel(manager).toString(),
                String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static ComponentName component(ResolveInfo app) {
        return new ComponentName(app.activityInfo.packageName, app.activityInfo.name);
    }

    private void addStoredApp(GridLayout grid, String key, Runnable remove) {
        ComponentName component = ComponentName.unflattenFromString(key);
        if (component == null) {
            return;
        }
        PackageManager manager = getPackageManager();
        try {
            ActivityInfo info = manager.getActivityInfo(component, 0);
            CharSequence name = info.loadLabel(manager);
            addAppTile(grid, info, name, () -> launch(component),
                    () -> confirm("App entfernen", name, remove));
        } catch (PackageManager.NameNotFoundException ignored) {
            addTile(grid, component.getShortClassName(), null,
                    () -> notifyUser("App nicht verfügbar"),
                    () -> confirm("App entfernen", component.getShortClassName(), remove));
        }
    }

    private void addAppTile(GridLayout grid, ActivityInfo info, CharSequence name,
            Runnable open, Runnable menu) {
        addTile(grid, name, info.loadIcon(getPackageManager()), open, menu);
    }

    private void addTile(GridLayout grid, CharSequence name,
            android.graphics.drawable.Drawable icon, Runnable open, Runnable menu) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        tile.setPadding(dp(3), dp(8), dp(3), dp(5));
        GridLayout.LayoutParams layout = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
        layout.width = 0;
        layout.height = dp(102);
        grid.addView(tile, layout);
        if (icon != null) {
            ImageView image = new ImageView(this);
            image.setImageDrawable(icon);
            tile.addView(image, dp(46), dp(46));
        } else {
            TextView folder = label("▦", 29);
            folder.setGravity(Gravity.CENTER);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(TILE);
            bg.setCornerRadius(dp(14));
            folder.setBackground(bg);
            tile.addView(folder, dp(46), dp(46));
        }
        TextView text = label(name, 12);
        text.setGravity(Gravity.CENTER);
        text.setMaxLines(2);
        text.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tile.addView(text, -1, -2);
        tile.setContentDescription(name);
        tile.setFocusable(true);
        tile.setOnClickListener(view -> open.run());
        tile.setOnLongClickListener(view -> {
            menu.run();
            return true;
        });
    }

    private void launch(ComponentName component) {
        try {
            startActivity(Intent.makeMainActivity(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (android.content.ActivityNotFoundException | SecurityException error) {
            notifyUser("App kann nicht geöffnet werden");
        }
    }

    private void pinMenu(ComponentName component) {
        String key = component.flattenToString();
        ArrayList<CharSequence> names = new ArrayList<>();
        ArrayList<JSONObject> folders = new ArrayList<>();
        names.add("Zum Startbildschirm");
        for (int i = 0; i < home.length(); i++) {
            JSONObject item = home.optJSONObject(i);
            if ("folder".equals(item.optString("kind"))) {
                folders.add(item);
                names.add("Ordner: " + item.optString("name"));
            }
        }
        new AlertDialog.Builder(this).setTitle("App hinzufügen")
                .setItems(names.toArray(new CharSequence[0]), (dialog, selection) -> {
                    if (selection == 0) {
                        for (int i = 0; i < home.length(); i++) {
                            if (key.equals(home.optJSONObject(i).optString("component"))) {
                                notifyUser("App ist bereits angeheftet");
                                return;
                            }
                        }
                        home.put(entry("app", "component", key));
                    } else {
                        JSONArray entries = folders.get(selection - 1).optJSONArray("apps");
                        for (int i = 0; i < entries.length(); i++) {
                            if (key.equals(entries.optString(i))) {
                                notifyUser("App befindet sich bereits im Ordner");
                                return;
                            }
                        }
                        entries.put(key);
                    }
                    saveAndRender();
                    notifyUser("App hinzugefügt");
                }).show();
    }

    private void folderName(JSONObject folder) {
        EditText input = new EditText(this);
        input.setSingleLine();
        input.setHint("Ordnername");
        if (folder != null) {
            input.setText(folder.optString("name"));
            input.selectAll();
        }
        new AlertDialog.Builder(this).setTitle(folder == null ? "Neuer Ordner" : "Ordner umbenennen")
                .setView(input).setNegativeButton("Abbrechen", null)
                .setPositiveButton("Speichern", (dialog, button) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        notifyUser("Ordnername fehlt");
                        return;
                    }
                    if (folder == null) {
                        JSONObject added = entry("folder", "name", name);
                        put(added, "apps", new JSONArray());
                        home.put(added);
                    } else {
                        put(folder, "name", name);
                    }
                    saveAndRender();
                }).show();
    }

    private void homeMenu(int index) {
        JSONObject entry = home.optJSONObject(index);
        new AlertDialog.Builder(this).setTitle(entry.optString("name"))
                .setItems(new String[]{"Umbenennen", "Ordner entfernen"}, (dialog, selection) -> {
                    if (selection == 0) {
                        folderName(entry);
                    } else {
                        confirm("Ordner entfernen", entry.optString("name"), () -> {
                            home.remove(index);
                            saveAndRender();
                        });
                    }
                }).show();
    }

    private void widgetMenu() {
        List<AppWidgetProviderInfo> providers = new ArrayList<>(widgetManager.getInstalledProviders());
        PackageManager manager = getPackageManager();
        providers.sort(Comparator.comparing(info -> info.loadLabel(manager),
                String.CASE_INSENSITIVE_ORDER));
        ArrayList<CharSequence> names = new ArrayList<>();
        for (AppWidgetProviderInfo provider : providers) {
            names.add(provider.loadLabel(manager));
        }
        if (names.isEmpty()) {
            notifyUser("Keine Widgets verfügbar");
            return;
        }
        new AlertDialog.Builder(this).setTitle("Widget hinzufügen")
                .setItems(names.toArray(new CharSequence[0]),
                        (dialog, selection) -> bindWidget(providers.get(selection))).show();
    }

    private void bindWidget(AppWidgetProviderInfo provider) {
        pendingWidget = widgetHost.allocateAppWidgetId();
        if (widgetManager.bindAppWidgetIdIfAllowed(pendingWidget, provider.provider)) {
            configureWidget();
            return;
        }
        Intent request = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidget)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider);
        try {
            startActivityForResult(request, BIND_WIDGET);
        } catch (android.content.ActivityNotFoundException error) {
            discardPendingWidget();
            notifyUser("Systemdialog für Widgets nicht verfügbar");
        }
    }

    private void configureWidget() {
        AppWidgetProviderInfo provider = widgetManager.getAppWidgetInfo(pendingWidget);
        if (provider == null) {
            discardPendingWidget();
        } else if (provider.configure != null) {
            try {
                widgetHost.startAppWidgetConfigureActivityForResult(
                        this, pendingWidget, 0, CONFIGURE_WIDGET, null);
            } catch (RuntimeException error) {
                discardPendingWidget();
                notifyUser("Widget-Konfiguration nicht verfügbar");
            }
        } else {
            saveWidget();
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (pendingWidget < 0 || (request != BIND_WIDGET && request != CONFIGURE_WIDGET)) {
            return;
        }
        if (result != RESULT_OK) {
            discardPendingWidget();
        } else if (request == BIND_WIDGET) {
            configureWidget();
        } else {
            saveWidget();
        }
    }

    private void saveWidget() {
        home.put(entry("widget", "id", pendingWidget));
        pendingWidget = -1;
        saveAndRender();
    }

    private void discardPendingWidget() {
        widgetHost.deleteAppWidgetId(pendingWidget);
        pendingWidget = -1;
    }

    private void addWidgetView(LinearLayout content, int id) {
        AppWidgetProviderInfo info = widgetManager.getAppWidgetInfo(id);
        String name = info == null ? "Widget nicht verfügbar" : info.loadLabel(getPackageManager());
        if (info != null) {
            AppWidgetHostView widget = widgetHost.createView(this, id, info);
            content.addView(widget, -1, Math.max(info.minHeight, dp(84)));
        }
        content.addView(button(name + " entfernen", () -> confirm("Widget entfernen", name, () -> {
            for (int i = 0; i < home.length(); i++) {
                if (home.optJSONObject(i).optInt("id", -1) == id) {
                    home.remove(i);
                    widgetHost.deleteAppWidgetId(id);
                    saveAndRender();
                    break;
                }
            }
        })));
    }

    private void confirm(String action, CharSequence name, Runnable confirmed) {
        new AlertDialog.Builder(this).setTitle(action).setMessage(name)
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Entfernen", (dialog, button) -> confirmed.run()).show();
    }

    private static JSONObject entry(String kind, String key, Object value) {
        JSONObject entry = new JSONObject();
        put(entry, "kind", kind);
        put(entry, key, value);
        return entry;
    }

    private static void put(JSONObject object, String key, Object value) {
        try {
            object.put(key, value);
        } catch (JSONException error) {
            throw new IllegalArgumentException(error);
        }
    }

    private void saveAndRender() {
        getPreferences(0).edit().putString("home", home.toString()).apply();
        render();
    }

    private TextView label(CharSequence name, int size) {
        TextView view = new TextView(this);
        view.setText(name);
        view.setTextColor(Color.BLACK);
        view.setTextSize(size);
        return view;
    }

    private Button button(String name, Runnable action) {
        Button button = new Button(this);
        button.setText(name);
        button.setTextColor(Color.BLACK);
        button.setAllCaps(false);
        button.setOnClickListener(view -> action.run());
        return button;
    }

    private void notifyUser(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
