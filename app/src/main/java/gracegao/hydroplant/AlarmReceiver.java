package gracegao.hydroplant;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static android.content.Context.ALARM_SERVICE;

// Custom class to schedule a water reset at midnight
public class AlarmReceiver extends BroadcastReceiver {

    // Called when the AlarmReceiver reaches the scheduled time
    @Override
    public void onReceive(Context context, Intent i) {
        // Resets water amount in phone storage
        SharedPreferences.Editor editor = context.getSharedPreferences("HYDROPLANT", Context.MODE_PRIVATE).edit();
        editor.putInt("WATER", 0);
        editor.commit();
    }

    // Class method that schedules a reset at midnight everyday
    public static void setReset(Context context){

        // Intent to execute
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Sets the scheduled task for midnight
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_MONTH, 1);

        // Sets the time and interval
        long midnight = calendar.getTimeInMillis();
        long interval = AlarmManager.INTERVAL_DAY;

        // Starts the AlarmManager service to repeat at midnight everyday
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        am.setInexactRepeating(
                AlarmManager.RTC,
                midnight,
                interval,
                pendingIntent);
    }
}
