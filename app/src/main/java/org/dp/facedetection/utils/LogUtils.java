/*
 * Copyright (C) 2010 mAPPn.Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dp.facedetection.utils;

import android.util.Log;

import org.dp.facedetection.BuildConfig;


/**
 * Common DateConvertUtils for the application
 */
public class LogUtils {

    public static final boolean sLogShow = BuildConfig.isDebug;
    public static String sLogTag = "HeMedical";

    public static void v(String msg) {
        if (sLogShow && msg != null) {
            Log.v(sLogTag, msg);
        }
    }

    public static void v(String tag, String msg) {
        if (sLogShow && msg != null) {
            Log.v(tag, msg);
        }
    }


    public static void d(String msg) {
        if (sLogShow && msg != null) {
            Log.d(sLogTag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (sLogShow && msg != null) {
            Log.d(tag, msg);
        }
    }


    public static void i(String msg) {
        if (sLogShow && msg != null) {
            Log.i(sLogTag, msg);
        }
    }


    public static void i(String tag, String msg) {
        if (sLogShow && msg != null) {
            Log.i(tag, msg);
        }
    }

    public static void w(String msg) {
        if (sLogShow && msg != null) {
            Log.w(sLogTag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (sLogShow && msg != null) {
            Log.w(tag, msg);
        }
    }


    public static void e(String msg) {
        if (sLogShow && msg != null) {
            Log.e(sLogTag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (sLogShow && msg != null) {
            Log.e(tag, msg);
        }
    }





}