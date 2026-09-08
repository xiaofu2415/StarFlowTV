package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;

import org.jetbrains.annotations.NotNull;

public final class LiveConfigUpdateDialog extends BaseDialog {
    private final ProgressBar progressBar;
    private final TextView progressText;
    private final TextView stageText;

    public LiveConfigUpdateDialog(@NonNull @NotNull Context context) {
        super(context);
        setContentView(R.layout.dialog_live_config_update);
        setCanceledOnTouchOutside(false);
        setCancelable(true);
        progressBar = findViewById(R.id.liveConfigProgress);
        progressText = findViewById(R.id.liveConfigProgressText);
        stageText = findViewById(R.id.liveConfigStageText);
    }

    public void updateProgress(int percent, String message) {
        int boundedPercent = Math.max(0, Math.min(100, percent));
        progressBar.setProgress(boundedPercent);
        progressText.setText(boundedPercent + "%");
        stageText.setText(message == null ? "正在更新直播源" : message);
    }
}
