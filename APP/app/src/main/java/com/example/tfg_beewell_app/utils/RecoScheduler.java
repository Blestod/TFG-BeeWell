// RecoScheduler.java
package com.example.tfg_beewell_app.utils;

import android.content.Context;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public final class RecoScheduler {

    private static long delayUntil(int hour) {
        Calendar now = Calendar.getInstance();
        Calendar target = (Calendar) now.clone();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1);
        return target.getTimeInMillis() - now.getTimeInMillis();
    }

    public static void scheduleDaily(Context ctx) {
        scheduleOne(ctx, 6,  "reco_6");
        scheduleOne(ctx, 12, "reco_12");
        scheduleOne(ctx, 16, "reco_16");
    }

    /* Re-enqueue itself every run ― done at the end of Worker via unique name */
    private static void scheduleOne(Context ctx, int hour, String tag) {
        long delay = delayUntil(hour);

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(RecoWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(tag)
                .build();

        WorkManager.getInstance(ctx)
                .enqueueUniqueWork(tag,  // unique name per clock-time
                        androidx.work.ExistingWorkPolicy.REPLACE,
                        req);
    }
}
