package com.sabev.neatologger;

import android.os.Bundle;

public class BundleFormatter {
    public static final String exceptionKey = "com.sabev.neatologger.exceptionKey";

    public Bundle exceptionBundle(Exception e) {
        Bundle b = new Bundle();
        putException(b, e);
        return b;
    }

    public void putException(Bundle b, Exception e) {
        b.putSerializable(exceptionKey, e);
    }

    public Exception getException(Bundle b) {
        return (Exception)b.getSerializable(exceptionKey);
    }
}
