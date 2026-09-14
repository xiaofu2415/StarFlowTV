from pathlib import Path

activity = Path('app/src/main/java/com/github/tvbox/osc/ui/activity/LivePlayActivity.java')
text = activity.read_text(encoding='utf-8')
old = '''private void ensureOfficialQualitySettingGroup() {
    if (liveSettingGroupList.size() > OFFICIAL_QUALITY_GROUP_INDEX) return;
    if (liveSettingGroupList.size() != OFFICIAL_QUALITY_GROUP_INDEX) return;
    LiveSettingGroup group = new LiveSettingGroup();
    group.setGroupIndex(OFFICIAL_QUALITY_GROUP_INDEX);
    group.setGroupName("官方源画质");
    ArrayList<LiveSettingItem> items = new ArrayList<>();
    for (int i = 0; i < OFFICIAL_QUALITY_LABELS.length; i++) {
        LiveSettingItem item = new LiveSettingItem();
        item.setItemIndex(i);
        item.setItemName(OFFICIAL_QUALITY_LABELS[i]);
        items.add(item);
    }
    group.setLiveSettingItems(items);
    liveSettingGroupList.add(group);
}

private int getOfficialQualityIndex(String value) {
    if (value != null) {
        for (int i = 0; i < OFFICIAL_QUALITY_VALUES.length; i++) {
            if (OFFICIAL_QUALITY_VALUES[i].equals(value)) return i;
        }
    }
    return 0;
}

private void applyOfficialQualitySelection(int position) {
    if (position < 0 || position >= OFFICIAL_QUALITY_VALUES.length) return;
    String value = OFFICIAL_QUALITY_VALUES[position];
    Hawk.put(HawkConfig.LIVE_OFFICIAL_QUALITY, value);
    liveSettingItemAdapter.selectItem(position, true, true);
    if (officialLiveController != null) officialLiveController.setQualityPreference(value);
    Toast.makeText(this, "官方源画质：" + OFFICIAL_QUALITY_LABELS[position], Toast.LENGTH_SHORT).show();
}
'''
new = '''    private void ensureOfficialQualitySettingGroup() {
        if (liveSettingGroupList.size() > OFFICIAL_QUALITY_GROUP_INDEX) return;
        if (liveSettingGroupList.size() != OFFICIAL_QUALITY_GROUP_INDEX) return;
        LiveSettingGroup group = new LiveSettingGroup();
        group.setGroupIndex(OFFICIAL_QUALITY_GROUP_INDEX);
        group.setGroupName("官方源画质");
        ArrayList<LiveSettingItem> items = new ArrayList<>();
        for (int i = 0; i < OFFICIAL_QUALITY_LABELS.length; i++) {
            LiveSettingItem item = new LiveSettingItem();
            item.setItemIndex(i);
            item.setItemName(OFFICIAL_QUALITY_LABELS[i]);
            items.add(item);
        }
        group.setLiveSettingItems(items);
        liveSettingGroupList.add(group);
    }

    private int getOfficialQualityIndex(String value) {
        if (value != null) {
            for (int i = 0; i < OFFICIAL_QUALITY_VALUES.length; i++) {
                if (OFFICIAL_QUALITY_VALUES[i].equals(value)) return i;
            }
        }
        return 0;
    }

    private void applyOfficialQualitySelection(int position) {
        if (position < 0 || position >= OFFICIAL_QUALITY_VALUES.length) return;
        String value = OFFICIAL_QUALITY_VALUES[position];
        Hawk.put(HawkConfig.LIVE_OFFICIAL_QUALITY, value);
        liveSettingItemAdapter.selectItem(position, true, true);
        if (officialLiveController != null) officialLiveController.setQualityPreference(value);
        Toast.makeText(this, "官方源画质：" + OFFICIAL_QUALITY_LABELS[position], Toast.LENGTH_SHORT).show();
    }
'''
if text.count(old) != 1:
    raise SystemExit('quality helper block did not match exactly once')
activity.write_text(text.replace(old, new, 1), encoding='utf-8')

test_file = Path('app/src/test/js/official-live-fullscreen.test.js')
tests = test_file.read_text(encoding='utf-8')
test_name = 'reports actual video resolution from the official player'
if test_name not in tests:
    tests += '''\n\ntest('reports actual video resolution from the official player', () => {
  const source = fs.readFileSync(scriptPath, 'utf8');
  const harness = createHarness({ videoWidth: 1920, videoHeight: 1080 });

  vm.runInNewContext(source, harness.context);

  assert.equal(
    harness.context.window.__starflowOfficialFullscreen.getResolution(),
    '1920×1080'
  );
});\n'''
test_file.write_text(tests, encoding='utf-8')
