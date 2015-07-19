package com.ashbysoft.felixstoweradio;

import android.content.Context;

import org.apache.commons.io.IOUtils;

import java.io.IOException;

public class Utils {
    public static String getStringFromRaw(Context context, int raw) throws IOException {
        return IOUtils.toString(context.getResources().openRawResource(raw));
    }
}
