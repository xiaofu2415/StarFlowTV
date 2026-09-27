(function () {
  'use strict';

  var API_NAME = '__starflowOfficialFullscreen';
  var ROOT_CLASS = 'starflow-official-fullscreen';
  var STYLE_ID = 'starflow-official-fullscreen-style';
  var QUALITY_RETRY_MS = 500;
  var QUALITY_MAX_RETRIES = 20;
  var QUALITY_STABILITY_MS = 8000;
  var QUALITY_MONITOR_MS = 1000;
  var QUALITY_FALLBACK_COOLDOWN_MS = 30000;

  if (window[API_NAME]) {
    window[API_NAME].enter();
    return;
  }

  var observer = null;
  var retryTimer = null;
  var qualityRetryTimer = null;
  var qualityRetryCount = 0;
  var qualityPreference = 'highest';
  var qualityAppliedPreference = null;
  var qualityMonitorTimer = null;
  var firstFrameSeen = false;
  var qualityStableSince = null;
  var lastObservedCurrentTime = null;
  var lastTotalFrames = null;
  var lastDroppedFrames = null;
  var videoWaiting = false;
  var selectedQualityRank = 0;
  var manuallySelectedQuality = false;
  var qualityCooldownUntil = 0;
  var qualityFallbackCount = 0;
  var qualityEscalationBlocked = false;
  var nativeFullscreenAttempted = false;
  var boundVideo = null;
  var fullscreenActive = false;

  function findPlayer() {
    return document.getElementById('player')
      || document.querySelector('.video_box')
      || document.querySelector('[data-play="zhibo"]');
  }

  function ensureStyle() {
    var style = document.getElementById(STYLE_ID);
    if (style) return style;
    style = document.createElement('style');
    style.id = STYLE_ID;
    style.textContent = [
      'html.' + ROOT_CLASS + ',html.' + ROOT_CLASS + ' body{margin:0!important;padding:0!important;width:100%!important;height:100%!important;overflow:hidden!important;background:#000!important;}',
      'html.' + ROOT_CLASS + ' [data-starflow-fullscreen="true"]{position:fixed!important;inset:0!important;left:0!important;top:0!important;width:100vw!important;height:100vh!important;max-width:none!important;margin:0!important;padding:0!important;z-index:2147483646!important;background:#000!important;}',
      'html.' + ROOT_CLASS + ' [data-starflow-fullscreen="true"],html.' + ROOT_CLASS + ' [data-starflow-fullscreen="true"] *{box-sizing:border-box!important;max-width:none!important;}',
      'html.' + ROOT_CLASS + ' [data-starflow-fullscreen="true"]>div,html.' + ROOT_CLASS + ' [data-starflow-fullscreen="true"] #html5Player_live,html.' + ROOT_CLASS + ' [data-starflow-fullscreen="true"] #html5Player,html.' + ROOT_CLASS + ' [data-starflow-fullscreen="true"] #html5VideoBack,html.' + ROOT_CLASS + ' [data-starflow-fullscreen="true"] #html5ControlDiv,html.' + ROOT_CLASS + ' [data-starflow-fullscreen="true"] #h5canvas_player{width:100%!important;height:100%!important;margin:0!important;}',
      'html.' + ROOT_CLASS + ' [data-starflow-fullscreen="true"] video,html.' + ROOT_CLASS + ' [data-starflow-fullscreen="true"] canvas,html.' + ROOT_CLASS + ' [data-starflow-fullscreen="true"] object,html.' + ROOT_CLASS + ' [data-starflow-fullscreen="true"] embed{position:absolute!important;inset:0!important;width:100%!important;height:100%!important;object-fit:contain!important;background:#000!important;}'
    ].join('');
    document.head.appendChild(style);
    return style;
  }

  function normalizedText(node) {
    if (!node) return '';
    var text = node.innerText || node.textContent || '';
    return String(text).replace(/\s+/g, '').toUpperCase();
  }

  function qualityRank(text) {
    if (/^(4K|2160P?|蓝光|原画|UHD)$/.test(text)) return 500;
    if (/^(1080P?|超清|超高清|FHD)$/.test(text)) return 400;
    if (/^(720P?|高清|HD)$/.test(text)) return 300;
    if (/^(576P?|480P?|标清|SD)$/.test(text)) return 200;
    if (/^(360P?|流畅|低清)$/.test(text)) return 100;
    if (/^(自动|默认|AUTO)$/.test(text)) return 0;
    return -1;
  }

  function qualityTargetRank(preference) {
    if (preference === '1080p') return 400;
    if (preference === '720p') return 300;
    if (preference === 'smooth') return 100;
    return 999;
  }

  function qualityCandidates() {
    var selector = [
      '[data-quality]', '[data-definition]', '[data-clarity]',
      '[class*="quality"]', '[class*="definition"]', '[class*="clarity"]',
      'button', 'a', 'li', 'span', 'div'
    ].join(',');
    var nodes = document.querySelectorAll(selector);
    var result = [];
    for (var i = 0; i < nodes.length; i += 1) {
      var node = nodes[i];
      var text = normalizedText(node);
      var rank = qualityRank(text);
      if (rank < 0) continue;
      if (node.hidden === true) continue;
      result.push({ node: node, text: text, rank: rank });
    }
    return result;
  }

  function resolutionRank(height) {
    var value = Number(height) || 0;
    if (value >= 2160) return 500;
    if (value >= 1080) return 400;
    if (value >= 720) return 300;
    if (value >= 480) return 200;
    if (value >= 360) return 100;
    return 0;
  }

  function qualityCeilingRank(preference) {
    if (preference === '1080p') return 400;
    if (preference === '720p') return 300;
    if (preference === 'smooth') return 100;
    return 500;
  }

  function chooseNextQualityCandidate(preference) {
    var candidates = qualityCandidates();
    if (!candidates.length) return undefined;
    var ceiling = qualityCeilingRank(preference);
    var lowerToPreference = preference !== 'highest'
      && !manuallySelectedQuality && selectedQualityRank > ceiling;
    var eligible = candidates.filter(function (candidate) {
      if (candidate.rank <= 0 || candidate.rank > ceiling) return false;
      return lowerToPreference || candidate.rank > selectedQualityRank;
    });
    if (!eligible.length) return null;
    eligible.sort(function (a, b) {
      return lowerToPreference ? b.rank - a.rank : a.rank - b.rank;
    });
    return eligible[0];
  }

  function chooseAdaptiveCandidate() {
    var candidates = qualityCandidates().filter(function (candidate) {
      return candidate.rank === 0;
    });
    return candidates.length ? candidates[0] : null;
  }

  function bufferAheadSeconds(video) {
    try {
      var ranges = video && video.buffered;
      var current = Number(video.currentTime);
      if (!ranges || !ranges.length || !isFinite(current)) return null;
      for (var i = ranges.length - 1; i >= 0; i -= 1) {
        var start = ranges.start(i);
        var end = ranges.end(i);
        if (current >= start - 0.25 && current <= end + 0.25) {
          return Math.max(0, end - current);
        }
      }
    } catch (ignored) {
      return null;
    }
    return null;
  }

  function readFrameStats(video) {
    try {
      if (video && typeof video.getVideoPlaybackQuality === 'function') {
        var stats = video.getVideoPlaybackQuality();
        return {
          total: Number(stats.totalVideoFrames) || 0,
          dropped: Number(stats.droppedVideoFrames) || 0
        };
      }
    } catch (ignored) {
      // Older WebViews may not expose frame-drop statistics.
    }
    return null;
  }

  function clearQualityRetry() {
    if (qualityRetryTimer) {
      clearTimeout(qualityRetryTimer);
      qualityRetryTimer = null;
    }
  }

  function clearQualityMonitor() {
    if (qualityMonitorTimer) {
      clearInterval(qualityMonitorTimer);
      qualityMonitorTimer = null;
    }
  }

  function resetQualityObservation(video) {
    firstFrameSeen = false;
    qualityStableSince = null;
    lastObservedCurrentTime = null;
    lastTotalFrames = null;
    lastDroppedFrames = null;
    videoWaiting = !!(video && video.paused);
    selectedQualityRank = resolutionRank(video && video.videoHeight);
    manuallySelectedQuality = false;
    qualityCooldownUntil = 0;
    qualityFallbackCount = 0;
    qualityEscalationBlocked = false;
    qualityAppliedPreference = null;
    qualityRetryCount = 0;
    clearQualityRetry();
    clearQualityMonitor();
  }

  function markFirstVideoFrame(video) {
    if (!fullscreenActive || !video || video !== boundVideo || firstFrameSeen) return;
    firstFrameSeen = true;
    videoWaiting = false;
    qualityStableSince = Date.now();
    lastObservedCurrentTime = Number(video.currentTime) || 0;
    var stats = readFrameStats(video);
    lastTotalFrames = stats ? stats.total : null;
    lastDroppedFrames = stats ? stats.dropped : null;
    if (!qualityMonitorTimer) {
      qualityMonitorTimer = setInterval(monitorQuality, QUALITY_MONITOR_MS);
    }
    applyQuality();
  }

  function fallbackAfterStall(video) {
    if (!manuallySelectedQuality) return;
    var candidate = chooseAdaptiveCandidate();
    if (!candidate) {
      var lower = qualityCandidates().filter(function (item) {
        return item.rank > 0 && item.rank < selectedQualityRank;
      }).sort(function (a, b) { return a.rank - b.rank; });
      candidate = lower.length ? lower[0] : null;
    }
    qualityFallbackCount += 1;
    if (!candidate || typeof candidate.node.click !== 'function') {
      qualityEscalationBlocked = true;
      manuallySelectedQuality = false;
      return;
    }
    candidate.node.click();
    manuallySelectedQuality = candidate.rank !== 0;
    selectedQualityRank = candidate.rank === 0
      ? resolutionRank(video && video.videoHeight) : candidate.rank;
    qualityAppliedPreference = null;
    qualityCooldownUntil = Date.now() + QUALITY_FALLBACK_COOLDOWN_MS;
    if (qualityFallbackCount >= 2) qualityEscalationBlocked = true;
  }

  function markVideoUnstable(video) {
    if (!video || video !== boundVideo) return;
    videoWaiting = true;
    qualityStableSince = null;
    fallbackAfterStall(video);
  }

  function monitorQuality() {
    var video = boundVideo;
    if (!fullscreenActive || !firstFrameSeen || !video) return;
    var now = Date.now();
    var currentTime = Number(video.currentTime);
    var advancing = lastObservedCurrentTime !== null
      && isFinite(currentTime) && currentTime > lastObservedCurrentTime + 0.02;
    lastObservedCurrentTime = isFinite(currentTime) ? currentTime : null;

    var stable = advancing && !videoWaiting;
    var stats = readFrameStats(video);
    if (stats && lastTotalFrames !== null && stats.total > lastTotalFrames) {
      var frameDelta = stats.total - lastTotalFrames;
      var droppedDelta = Math.max(0, stats.dropped - (lastDroppedFrames || 0));
      if (droppedDelta / frameDelta > 0.08) stable = false;
    }
    if (stats) {
      lastTotalFrames = stats.total;
      lastDroppedFrames = stats.dropped;
    }
    var bufferedAhead = bufferAheadSeconds(video);
    if (bufferedAhead !== null && bufferedAhead < 0.5) stable = false;

    if (!stable) {
      qualityStableSince = null;
      return;
    }
    if (qualityStableSince === null) qualityStableSince = now;
    if (qualityPreference !== 'auto' && !qualityEscalationBlocked
        && now >= qualityCooldownUntil
        && now - qualityStableSince >= QUALITY_STABILITY_MS) {
      applyQuality();
    }
  }

  function applyQuality() {
    if (qualityPreference === 'auto') {
      clearQualityRetry();
      qualityAppliedPreference = 'auto';
      return true;
    }
    if (!firstFrameSeen || qualityEscalationBlocked) return false;
    var now = Date.now();
    if (now < qualityCooldownUntil || qualityStableSince === null
        || now - qualityStableSince < QUALITY_STABILITY_MS) return false;
    if (qualityAppliedPreference === qualityPreference) return true;
    var candidate = chooseNextQualityCandidate(qualityPreference);
    if (candidate === undefined) {
      scheduleQualityRetry();
      return false;
    }
    if (!candidate) return true;
    if (typeof candidate.node.click !== 'function') {
      scheduleQualityRetry();
      return false;
    }
    candidate.node.click();
    clearQualityRetry();
    selectedQualityRank = candidate.rank;
    manuallySelectedQuality = true;
    qualityStableSince = now;
    lastObservedCurrentTime = Number(boundVideo.currentTime) || 0;
    var stats = readFrameStats(boundVideo);
    lastTotalFrames = stats ? stats.total : null;
    lastDroppedFrames = stats ? stats.dropped : null;
    qualityAppliedPreference = selectedQualityRank >= qualityCeilingRank(qualityPreference)
      ? qualityPreference : null;
    return true;
  }

  function setQuality(preference) {
    var normalized = String(preference || '').toLowerCase();
    if (['highest', 'auto', '1080p', '720p', 'smooth'].indexOf(normalized) < 0) {
      normalized = 'highest';
    }
    if (normalized === qualityPreference) {
      applyQuality();
      return;
    }
    qualityPreference = normalized;
    qualityAppliedPreference = null;
    qualityRetryCount = 0;
    qualityFallbackCount = 0;
    qualityEscalationBlocked = false;
    qualityCooldownUntil = 0;
    clearQualityRetry();
    if (normalized === 'auto' && manuallySelectedQuality && boundVideo) {
      var adaptive = chooseAdaptiveCandidate();
      if (adaptive && typeof adaptive.node.click === 'function') adaptive.node.click();
      manuallySelectedQuality = false;
      selectedQualityRank = resolutionRank(boundVideo.videoHeight);
    }
    if (firstFrameSeen) {
      qualityStableSince = Date.now();
      lastObservedCurrentTime = Number(boundVideo.currentTime) || 0;
    }
    applyQuality();
  }

  function getResolution() {
    var player = findPlayer();
    var video = player && player.querySelector ? player.querySelector('video') : null;
    if (!video || !video.videoWidth || !video.videoHeight) return '';
    return String(video.videoWidth) + '×' + String(video.videoHeight);
  }

  function requestNativeFullscreen(video) {
    if (!fullscreenActive || !video || video !== boundVideo || nativeFullscreenAttempted
        || document.fullscreenElement || document.webkitFullscreenElement) return;
    var request = video.requestFullscreen || video.webkitRequestFullscreen;
    if (typeof request !== 'function') return;
    nativeFullscreenAttempted = true;
    try {
      var result = request.call(video);
      if (result && typeof result.catch === 'function') {
        result.catch(function () {});
      }
    } catch (ignored) {
      // CSS fullscreen remains active when native fullscreen is unavailable.
    }
  }

  function promotePlayer() {
    if (!fullscreenActive) return false;
    ensureStyle();
    document.documentElement.classList.add(ROOT_CLASS);
    var player = findPlayer();
    if (!player) return false;
    if (retryTimer) {
      clearInterval(retryTimer);
      retryTimer = null;
    }
    player.setAttribute('data-starflow-fullscreen', 'true');

    var video = player.querySelector('video');
    if (video && video !== boundVideo) {
      clearQualityMonitor();
      boundVideo = video;
      resetQualityObservation(video);
      nativeFullscreenAttempted = false;
      video.removeAttribute('playsinline');
      video.removeAttribute('webkit-playsinline');
      video.addEventListener('playing', function () {
        videoWaiting = false;
        requestNativeFullscreen(video);
        if (firstFrameSeen) {
          qualityStableSince = null;
          lastObservedCurrentTime = Number(video.currentTime) || 0;
        }
        applyQuality();
      });
      video.addEventListener('canplay', function () {
        videoWaiting = false;
        requestNativeFullscreen(video);
        if (typeof video.requestVideoFrameCallback !== 'function') {
          markFirstVideoFrame(video);
        }
        applyQuality();
      });
      video.addEventListener('waiting', function () { markVideoUnstable(video); });
      video.addEventListener('stalled', function () { markVideoUnstable(video); });
      if (typeof video.requestVideoFrameCallback === 'function') {
        var frameCallbackScheduled = false;
        try {
          var frameCallbackId = video.requestVideoFrameCallback(
            function () { markFirstVideoFrame(video); });
          frameCallbackScheduled = typeof frameCallbackId === 'number';
        } catch (ignored) {
          frameCallbackScheduled = false;
        }
        if (!frameCallbackScheduled) {
          video.addEventListener('loadeddata', function () { markFirstVideoFrame(video); }, { once: true });
        }
      } else {
        video.addEventListener('loadeddata', function () { markFirstVideoFrame(video); }, { once: true });
      }
    }
    if (video && !video.paused) requestNativeFullscreen(video);
    applyQuality();
    return true;
  }

  function enter() {
    if (!fullscreenActive) {
      fullscreenActive = true;
      nativeFullscreenAttempted = false;
      boundVideo = null;
    }
    ensureStyle();
    var promoted = promotePlayer();
    if (!observer && typeof MutationObserver === 'function') {
      observer = new MutationObserver(function () {
        promotePlayer();
        applyQuality();
      });
      observer.observe(document.body || document.documentElement, {
        childList: true,
        subtree: true
      });
    }
    if (!promoted && !retryTimer) retryTimer = setInterval(promotePlayer, 500);
  }

  function exit() {
    fullscreenActive = false;
    document.documentElement.classList.remove(ROOT_CLASS);
    var player = findPlayer();
    if (player) player.removeAttribute('data-starflow-fullscreen');
    if (observer) {
      observer.disconnect();
      observer = null;
    }
    if (retryTimer) {
      clearInterval(retryTimer);
      retryTimer = null;
    }
    clearQualityRetry();
    clearQualityMonitor();
    qualityRetryCount = 0;
    qualityAppliedPreference = null;
    firstFrameSeen = false;
    qualityStableSince = null;
    lastObservedCurrentTime = null;
    nativeFullscreenAttempted = false;
    boundVideo = null;
    var exitFullscreen = document.exitFullscreen || document.webkitExitFullscreen;
    if ((document.fullscreenElement || document.webkitFullscreenElement)
        && typeof exitFullscreen === 'function') {
      try {
        exitFullscreen.call(document);
      } catch (ignored) {
        // The CSS fullscreen layer has already been removed.
      }
    }
  }

  window[API_NAME] = {
    enter: enter,
    exit: exit,
    setQuality: setQuality,
    getQuality: function () { return qualityPreference; },
    getResolution: getResolution
  };
  enter();
}());
