package io.github.graphenelauncherplus.mini.testwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

public final class TestWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] widgetIds) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.fixture);
        manager.updateAppWidget(widgetIds, views);
    }
}
