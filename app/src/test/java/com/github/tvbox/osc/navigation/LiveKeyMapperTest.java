package com.github.tvbox.osc.navigation;

import android.view.KeyEvent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class LiveKeyMapperTest {
    private final LiveKeyMapper mapper = new LiveKeyMapper();

    @Test
    public void verticalKeysChangeChannels() {
        assertEquals(LiveKeyAction.CHANNEL_PREVIOUS,
                mapper.map(KeyEvent.KEYCODE_DPAD_UP, false));
        assertEquals(LiveKeyAction.CHANNEL_NEXT,
                mapper.map(KeyEvent.KEYCODE_DPAD_DOWN, false));
    }

    @Test
    public void horizontalKeysChangeLines() {
        assertEquals(LiveKeyAction.LINE_PREVIOUS,
                mapper.map(KeyEvent.KEYCODE_DPAD_LEFT, false));
        assertEquals(LiveKeyAction.LINE_NEXT,
                mapper.map(KeyEvent.KEYCODE_DPAD_RIGHT, false));
    }

    @Test
    public void confirmOpensChannelList() {
        assertEquals(LiveKeyAction.OPEN_CHANNELS,
                mapper.map(KeyEvent.KEYCODE_DPAD_CENTER, false));
        assertEquals(LiveKeyAction.OPEN_CHANNELS,
                mapper.map(KeyEvent.KEYCODE_ENTER, false));
    }

    @Test
    public void menuOpensSettings() {
        assertEquals(LiveKeyAction.OPEN_SETTINGS,
                mapper.map(KeyEvent.KEYCODE_MENU, false));
        assertEquals(LiveKeyAction.OPEN_SETTINGS,
                mapper.map(KeyEvent.KEYCODE_INFO, false));
    }

    @Test
    public void guideAndTvKeysOpenVod() {
        assertEquals(LiveKeyAction.OPEN_VOD,
                mapper.map(KeyEvent.KEYCODE_GUIDE, false));
        assertEquals(LiveKeyAction.OPEN_VOD,
                mapper.map(KeyEvent.KEYCODE_TV, false));
    }

    @Test
    public void backIsExplicit() {
        assertEquals(LiveKeyAction.BACK,
                mapper.map(KeyEvent.KEYCODE_BACK, false));
    }

    @Test
    public void visibleMenusReceiveNavigation() {
        assertEquals(LiveKeyAction.NONE,
                mapper.map(KeyEvent.KEYCODE_DPAD_UP, true));
        assertEquals(LiveKeyAction.NONE,
                mapper.map(KeyEvent.KEYCODE_DPAD_CENTER, true));
        assertEquals(LiveKeyAction.NONE,
                mapper.map(KeyEvent.KEYCODE_MENU, true));
    }
}
