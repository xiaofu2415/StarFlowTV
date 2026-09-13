package com.github.tvbox.osc.update;

public final class UpdateCheckResult {
    public enum Status {
        BUSY,
        UPDATE_AVAILABLE,
        NO_UPDATE,
        NETWORK_ERROR,
        SIGNATURE_INVALID,
        INVALID_MANIFEST,
        UNSUPPORTED,
        ABI_UNAVAILABLE
    }

    public final Status status;
    public final String versionName;

    public UpdateCheckResult(Status status, String versionName) {
        this.status = status;
        this.versionName = versionName;
    }

    public static String message(Status status, String versionName) {
        if (status == null) return "更新检查失败，请稍后重试";
        switch (status) {
            case BUSY:
                return "正在检查更新，请稍候";
            case UPDATE_AVAILABLE:
                return versionName == null || versionName.trim().isEmpty()
                        ? "发现新版本" : "发现新版本 " + versionName;
            case NO_UPDATE:
                return "当前已是最新版本";
            case NETWORK_ERROR:
                return "更新检查失败，请检查网络连接";
            case SIGNATURE_INVALID:
                return "更新签名校验失败，请稍后重试";
            case INVALID_MANIFEST:
                return "更新清单无效，请稍后重试";
            case UNSUPPORTED:
                return "当前设备暂不支持此更新";
            case ABI_UNAVAILABLE:
                return "没有适合当前设备的安装包";
            default:
                return "更新检查失败，请稍后重试";
        }
    }
}
