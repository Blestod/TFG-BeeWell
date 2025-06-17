/* ui/log/Utils.java */
package com.example.tfg_beewell_app.ui.log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Utils {
    private static final SimpleDateFormat DF =
            new SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault());
    public static String fmt(long epochSec){ return DF.format(new Date(epochSec*1000)); }
}

