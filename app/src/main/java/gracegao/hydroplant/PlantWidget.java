package gracegao.hydroplant;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * Implementation of App Widget functionality.
 */
public class PlantWidget extends AppWidgetProvider {

    private SharedPreferences settings;
    private int water, goal;
    private static final String ACTION_CLICK_MINUS = "ACTION_CLICK_MINUS";
    private static final String ACTION_CLICK_PLUS = "ACTION_CLICK_PLUS";
    private static final String ACTION_CLICK_POT = "ACTION_CLICK_POT";

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final ComponentName plantWidget = new ComponentName(context, PlantWidget.class);
        settings = context.getSharedPreferences("HYDROPLANT", Context.MODE_PRIVATE);
        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (key.equals("WATER")||key.equals("GOAL")){
                    updateWidget(context, appWidgetManager, plantWidget);
                }
            }
        };
        settings.registerOnSharedPreferenceChangeListener(listener);

        updateWidget(context, appWidgetManager, plantWidget);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        settings = context.getSharedPreferences("HYDROPLANT", Context.MODE_PRIVATE);
        water = settings.getInt("WATER", 0);
        if (ACTION_CLICK_MINUS.equals(intent.getAction())) {
            water -= 250;
            if (water < 0){
                water = 0;
            }
        }
        if (ACTION_CLICK_PLUS.equals(intent.getAction())) {
            water += 250;
            if (water > 9999) {
                water = 9999;
            }
        }
        changeWater(context, water);
    }

    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private void changeWater(Context context, int amount){
        SharedPreferences settings = context.getSharedPreferences("HYDROPLANT", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("WATER", amount);
        editor.commit();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName plantWidget = new ComponentName(context, PlantWidget.class);
        updateWidget(context, appWidgetManager, plantWidget);
    }

    public void updateWidget(Context context, AppWidgetManager appWidgetManager, ComponentName plantWidget)
    {
        settings = context.getSharedPreferences("HYDROPLANT", Context.MODE_PRIVATE);
        water = settings.getInt("WATER", 0);
        goal = settings.getInt("GOAL", 3000);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.plant_widget);
        remoteViews.setOnClickPendingIntent(R.id.minusWidget, getPendingSelfIntent(context, ACTION_CLICK_MINUS));
        remoteViews.setOnClickPendingIntent(R.id.plusWidget, getPendingSelfIntent(context, ACTION_CLICK_PLUS));

        Intent i = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, i, 0);
        remoteViews.setOnClickPendingIntent(R.id.potWidget, pi);

        remoteViews.setTextViewText(R.id.waterLabelWidget, Integer.toString(water));
        String goalLabel = "/ " + goal + " mL";
        remoteViews.setTextViewText(R.id.goalLabelWidget, goalLabel);

        int state = 5;
        // Gives feedback on water consumption
        if (water < (float)goal/4) {
            state = 1;
            if (water!=0){
                Toast.makeText(context, "you need more water!", Toast.LENGTH_LONG).show();
            }
        } else if (water < (float)goal/2) {
            state = 2;
            Toast.makeText(context, "keep it up!", Toast.LENGTH_LONG).show();
        } else if (water < (float)goal*3/4){
            state = 3;
            Toast.makeText(context, "keep going!", Toast.LENGTH_LONG).show();
        } else if (water < goal){
            state = 4;
            Toast.makeText(context, "almost there!", Toast.LENGTH_LONG).show();
        } else if (water < goal+((float)(10000-goal)/2)) {
            state = 5;
            Toast.makeText(context, "your plant is hydrated & happy", Toast.LENGTH_LONG).show();
        } else if ((water < goal+((float)(10000-goal)*3/4))) {
            state = 6;
            Toast.makeText(context, "be careful not to overwater!", Toast.LENGTH_LONG).show();
        } else if (water < 10000) {
            state = 7;
            Toast.makeText(context, "stop, your plant will drown!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "don't water more than 10L!", Toast.LENGTH_LONG).show();
            // Sets maximum water amount per day
            water = 9999;
        }

        // Changes plant image according to its hydration state
        remoteViews.setImageViewResource(R.id.plantWidgetImage, R.drawable.widget+(state));

        appWidgetManager.updateAppWidget(plantWidget, remoteViews);
    }
}

