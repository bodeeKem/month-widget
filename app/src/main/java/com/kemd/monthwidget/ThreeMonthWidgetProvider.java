package com.kemd.monthwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.widget.RemoteViews;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ThreeMonthWidgetProvider extends AppWidgetProvider {

    private static final int[] COLUMN_IDS = {
            R.id.month_column_0, R.id.month_column_1, R.id.month_column_2
    };
    private static final String[] DAY_LABELS = {"S", "M", "T", "W", "T", "F", "S"};

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_three_month);

            YearMonth current = YearMonth.now();
            for (int i = 0; i < 3; i++) {
                buildMonthColumn(context, views, COLUMN_IDS[i], current.plusMonths(i));
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private void buildMonthColumn(Context context, RemoteViews rootViews, int columnId, YearMonth month) {
        rootViews.removeAllViews(columnId);

        RemoteViews title = new RemoteViews(context.getPackageName(), R.layout.widget_month_title);
        String label = month.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault())
                + " " + month.getYear();
        title.setTextViewText(R.id.month_title_text, label);
        rootViews.addView(columnId, title);

        RemoteViews headerRow = new RemoteViews(context.getPackageName(), R.layout.widget_day_row);
        for (String d : DAY_LABELS) {
            RemoteViews cell = new RemoteViews(context.getPackageName(), R.layout.widget_day_cell);
            cell.setTextViewText(R.id.day_cell_text, d);
            headerRow.addView(R.id.day_row_container, cell);
        }
        rootViews.addView(columnId, headerRow);

        Set<Integer> daysWithEvents = getDaysWithEvents(context, month);

        LocalDate firstOfMonth = month.atDay(1);
        int startOffset = firstOfMonth.getDayOfWeek().getValue() % 7;
        int totalDays = month.lengthOfMonth();

        int dayCounter = 1;
        while (dayCounter <= totalDays) {
            RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_day_row);
            for (int col = 0; col < 7; col++) {
                RemoteViews cell = new RemoteViews(context.getPackageName(), R.layout.widget_day_cell);
                boolean isBlank = (dayCounter == 1 && col < startOffset) || dayCounter > totalDays;
                if (isBlank) {
                    cell.setTextViewText(R.id.day_cell_text, "");
                } else {
                    cell.setTextViewText(R.id.day_cell_text, String.valueOf(dayCounter));
                    if (daysWithEvents.contains(dayCounter)) {
                        cell.setInt(R.id.day_cell_text, "setBackgroundColor", 0xFF3A6EA5);
                    }
                    dayCounter++;
                }
            }
            rootViews.addView(columnId, row);
        }
    }

    private Set<Integer> getDaysWithEvents(Context context, YearMonth month) {
        Set<Integer> days = new HashSet<>();
        try {
            LocalDate start = month.atDay(1);
            LocalDate end = month.atEndOfMonth();
            long startMillis = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endMillis = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, startMillis);
            ContentUris.appendId(builder, endMillis);

            String[] projection = {CalendarContract.Instances.BEGIN};
            Cursor cursor = context.getContentResolver().query(
                    builder.build(), projection, null, null, null);

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long begin = cursor.getLong(0);
                    LocalDate d = Instant.ofEpochMilli(begin)
                            .atZone(ZoneId.systemDefault()).toLocalDate();
                    if (YearMonth.from(d).equals(month)) {
                        days.add(d.getDayOfMonth());
                    }
                }
                cursor.close();
            }
        } catch (SecurityException e) {
            // Permission not granted yet — widget just shows plain calendars
        }
        return days;
    }
}
