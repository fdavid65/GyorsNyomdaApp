package com.example.gyorsnyomdaapp; // <<< FIGYELEM: EZ A TE CSOMAGNEVED LEGYEN!

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log; // Logoláshoz

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import android.Manifest;

public class NapiErtesitoWorker extends Worker {

    private static final String CHANNEL_ID_NAPI = "NAPI_ERTESITO_CSATORNA";
    private static final int NOTIFICATION_ID_NAPI = 303; // Egyedi szám az értesítésnek

    public NapiErtesitoWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        Log.d("NapiErtesitoWorker", "Munka elindult!");

        // 1. Csatorna létrehozása (ha még nincs) - Ezt jobb lenne központilag, de itt is jó lesz egyszerűség kedvéért
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Napi Emlékeztetők";
            String description = "Fontos napi emlékeztetők";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_NAPI, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d("NapiErtesitoWorker", "Értesítési csatorna létrehozva/ellenőrizve: " + CHANNEL_ID_NAPI);
            }
        }

        // 2. Értesítés elkészítése
        // FONTOS: Az R.mipmap.ic_launcher helyett használj egy saját kis ikont!
        // Pl. android.R.drawable.ic_dialog_info teszteléshez jó.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_NAPI)
                .setSmallIcon(R.mipmap.ic_launcher) // <<<--- CSERÉLD LE EZT EGY SAJÁT KIS IKONRA!
                .setContentTitle("GyorsNyomdaApp Emlékeztető")
                .setContentText("Használtad ma már az alkalmazást?")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true); // Eltűnik kattintásra

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // 3. Engedély ellenőrzése (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("NapiErtesitoWorker", "Nincs értesítési engedély. Az értesítés nem lesz elküldve.");
                // A Workerből nem tudsz engedélyt kérni, ezt az Activity-ben kell megtenni.
                return Result.failure(); // Vagy success(), de nem küldtünk semmit
            }
        }

        // 4. Értesítés megjelenítése
        notificationManager.notify(NOTIFICATION_ID_NAPI, builder.build());
        Log.d("NapiErtesitoWorker", "Napi értesítés elküldve.");

        return Result.success(); // Jelezd, hogy a munka rendben lefutott
    }
}