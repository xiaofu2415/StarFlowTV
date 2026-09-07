package com.github.tvbox.osc.navigation;

import android.view.KeyEvent;

public final class LiveKeyMapper {
    public LiveKeyAction map(int keyCode, boolean menuVisible) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return LiveKeyAction.BACK;
        }
        if (menuVisible) {
            return LiveKeyAction.NONE;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                return LiveKeyAction.CHANNEL_PREVIOUS;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return LiveKeyAction.CHANNEL_NEXT;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return LiveKeyAction.LINE_PREVIOUS;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return LiveKeyAction.LINE_NEXT;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                return LiveKeyAction.OPEN_CHANNELS;
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_INFO:
            case KeyEvent.KEYCODE_HELP:
                return LiveKeyAction.OPEN_SETTINGS;
            case KeyEvent.KEYCODE_GUIDE:
            case KeyEvent.KEYCODE_TV:
            case KeyEvent.KEYCODE_MEDIA_TOP_MENU:
                return LiveKeyAction.OPEN_VOD;
            default:
                return LiveKeyAction.NONE;
        }
    }
}
