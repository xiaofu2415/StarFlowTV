package com.github.tvbox.osc.live;

public final class LineHealth {
    private final String lineId;
    private final int sourceIndex;
    private final int score;
    private final String codec;
    private final int height;
    private final long circuitOpenUntilMillis;
    private final long lastSuccessMillis;

    public LineHealth(String lineId, int sourceIndex, int score, String codec, int height,
                      long circuitOpenUntilMillis, long lastSuccessMillis) {
        this.lineId = lineId;
        this.sourceIndex = sourceIndex;
        this.score = score;
        this.codec = codec == null ? "" : codec;
        this.height = height;
        this.circuitOpenUntilMillis = circuitOpenUntilMillis;
        this.lastSuccessMillis = lastSuccessMillis;
    }

    public String getLineId() { return lineId; }
    public int getSourceIndex() { return sourceIndex; }
    int getScore() { return score; }
    String getCodec() { return codec; }
    int getHeight() { return height; }
    long getLastSuccessMillis() { return lastSuccessMillis; }
    boolean isCircuitOpen(long nowMillis) { return circuitOpenUntilMillis > nowMillis; }
}
