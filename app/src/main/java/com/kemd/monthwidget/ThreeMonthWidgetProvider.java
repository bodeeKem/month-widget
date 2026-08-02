package com.kemd.monthwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
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

    private static final String[] DAY_LABELS = {"S", "M", "T", "W", "T", "F", "S"};

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                           int appWidgetId, Bundle newOptions) {
        updateWidget(context, appWidgetManager, appWidgetId);
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 320);
        int heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 250);
        if (widthDp <= 0) widthDp = 320;
        if (heightDp <= 0) heightDp = 250;

        float density = Resources.getSystem().getDisplayMetrics().density;
        int widthPx = Math.round(widthDp * density);
        int heightPx = Math.round(heightDp * density);

        Bitmap bitmap = drawCalendarBitmap(context, widthPx, heightPx);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_three_month);
        views.setImageViewBitmap(R.id.widget_image, bitmap);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private Bitmap drawCalendarBitmap(Context context, int widthPx, int heightPx) {
        Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.parseColor("#1A1A1A"));

        float density = Resources.getSystem().getDisplayMetrics().density;
        float titleSize = 12 * density;
        float headerSize = 9 * density;
        float daySize = 10 * density;

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(titleSize);
        titlePaint.setFakeBoldText(true);
        titlePaint.setTextAlign(Paint.Align.CENTER);

        Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(Color.parseColor("#AAAAAA"));
        headerPaint.setTextSize(headerSize);
        headerPaint.setTextAlign(Paint.Align.CENTER);

        Paint dayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dayPaint.setColor(Color.parseColor("#DDDDDD"));
        dayPaint.setTextSize(daySize);
        dayPaint.setTextAlign(Paint.Align.CENTER);

        Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(Color.parseColor("#3A6EA5"));

        int columnWidth = widthPx / 3;
        YearMonth current = YearMonth.now();

        for (int i = 0; i < 3; i++) {
            YearMonth month = current.plusMonths(i);
            float colLeft = i * columnWidth;
            float colCenter = colLeft + columnWidth / 2f;
            float cellWidth = columnWidth / 7f;

            float y = titleSize + (6 * density);
            String label = month.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    + " " + month.getYear();
            canvas.drawText(label, colCenter, y, titlePaint);

            y += headerSize + (10 * density);
            for (int d = 0; d < 7; d++) {
                float x = colLeft + cellWidth * d + cellWidth / 2f;
                canvas.drawText(DAY_LABELS[d], x, y, headerPaint);
            }

            Set<Integer> daysWithEvents = getDaysWithEvents(context, month);

            LocalDate firstOfMonth = month.atDay(1);
            int startOffset = firstOfMonth.getDayOfWeek().getValue() % 7;
            int totalDays = month.lengthOfMonth();

            float rowHeight = daySize + (8 * density);
            int dayCounter = 1;
            int row = 0;
            while (dayCounter <= totalDays) {
                float rowY = y + headerSize + (6 * density) + (row * rowHeight);
                for (int col = 0; col < 7; col++) {
                    boolean isBlank = (dayCounter == 1 && col < startOffset) || dayCounter > totalDays;
                    if (!isBlank) {
                        float cx = colLeft + cellWidth * col + cellWidth / 2f;
                        if (daysWithEvents.contains(dayCounter)) {
                            RectF r = new RectF(cx - cellWidth / 2f + 2, rowY - daySize,
                                    cx + cellWidth / 2f - 2, rowY + (4 * density));
                            canvas.drawRoundRect(r, 4 * density, 4 * density, highlightPaint);
                        }
                        canvas.drawText(String.valueOf(dayCounter), cx, rowY, dayPaint);
                        dayCounter++;
                    }
                }
                row++;
            }
        }

        return bitmap;
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
            // permission not granted yet
        }
        return days;
    }
}
