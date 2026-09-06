package com.github.tvbox.osc.update;

public final class AbiSelector {
    private AbiSelector() {
    }

    public static UpdateManifest.Apk select(UpdateManifest manifest, String[] supportedAbis) {
        if (manifest == null || manifest.packages == null) return null;
        if (supportedAbis != null) {
            for (String deviceAbi : supportedAbis) {
                for (UpdateManifest.Apk apk : manifest.packages) {
                    if (apk != null && apk.abi != null && apk.abi.equals(deviceAbi)) return apk;
                }
            }
        }
        for (UpdateManifest.Apk apk : manifest.packages) {
            if (apk != null && "universal".equals(apk.abi)) return apk;
        }
        return null;
    }
}
