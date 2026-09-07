package com.github.tvbox.osc.live;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class DeviceCapabilities {
    private final int maxHeight;
    private final Set<String> supportedCodecs = new HashSet<>();

    public DeviceCapabilities(int maxHeight, Collection<String> supportedCodecs) {
        this.maxHeight = maxHeight;
        if (supportedCodecs != null) {
            for (String codec : supportedCodecs) {
                if (codec != null) this.supportedCodecs.add(codec.toLowerCase(Locale.US));
            }
        }
    }

    boolean supports(LineHealth line) {
        return line.getHeight() <= maxHeight
                && supportedCodecs.contains(line.getCodec().toLowerCase(Locale.US));
    }
}
