package com.github.tvbox.osc.live;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class LineSelector {
    public List<LineHealth> rank(List<LineHealth> lines, DeviceCapabilities device, long nowMillis) {
        List<LineHealth> ranked = new ArrayList<>(lines);
        ranked.sort(Comparator
                .comparing((LineHealth line) -> line.isCircuitOpen(nowMillis))
                .thenComparing(line -> !device.supports(line))
                .thenComparing(line -> line.getLastSuccessMillis() <= 0L)
                .thenComparing(Comparator.comparingInt(LineHealth::getScore).reversed())
                .thenComparing(Comparator.comparingLong(LineHealth::getLastSuccessMillis).reversed())
                .thenComparing(LineHealth::getLineId));
        return ranked;
    }
}
