(function () {
  'use strict';

  var API_NAME = '__starflowOfficialFullscreen';
  var ROOT_CLASS = 'starflow-official-fullscreen';
  var STYLE_ID = 'starflow-official-fullscreen-style';
  var QUALITY_RETRY_MS = 500;
  var QUALITY_MAX_RETRIES = 20;

  if (window[API_NAME]) {
    window[API_NAME].enter();
    return;
  }

  var observer = null;
  var retryTimer = null;
  var qualityRetryTimer = null;
  var qualityRetryCount = 0;
  var qualityPreference = 'auto';
  var qualityAppliedPreference = null;
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

  function chooseQualityCandidate(preference) {
    if (preference === 'auto') return null;
    var candidates = qualityCandidates();
    if (!candidates.length) return undefined;
    var target = qualityTargetRank(preference);
    var eligible = candidates.filter(function (candidate) {
      return preference === 'highest' ? candidate.rank > 0 : candidate.rank > 0 && candidate.rank <= target;
    });
    if (!eligible.length) return undefined;
    eligible.sort(function (a, b) { return b.rank - a.rank; });
    return eligible[0];
  }

  function clearQualityRetry() {
    if (qualityRetryTimer) {
      clearTimeout(qualityRetryTimer);
      qualityRetryTimer = null;
    }
  }

  function scheduleQualityRetry() {
    if (qualityPreference === 'auto' || qualityAppliedPreference === qualityPreference) return;
    if (qualityRetryTimer || qualityRetryCount >= QUALITY_MAX_RETRIES) return;
    qualityRetryCount += 1;
    qualityRetryTimer = setTimeout(function () {
      qualityRetryTimer = null;
      applyQuality();
    }, QUALITY_RETRY_MS);
  }

  function applyQuality() {
    if (qualityPreference === 'auto') {
      clearQualityRetry();
      qualityAppliedPreference = 'auto';
      return true;
    }
    if (qualityAppliedPreference === qualityPreference) return true;
    var candidate = chooseQualityCandidate(qualityPreference);
    if (candidate === undefined) {
      scheduleQualityRetry();
      return false;
    }
    if (!candidate || typeof candidate.node.click !== 'function') {
      scheduleQualityRetry();
      return false;
    }
    candidate.node.click();
    clearQualityRetry();
    qualityAppliedPreference = qualityPreference;
    return true;
  }

  function setQuality(preference) {
    var normalized = String(preference || '').toLowerCase();
    if (['highest', 'auto', '1080p', '720p', 'smooth'].indexOf(normalized) < 0) {
      normalized = 'highest';
    }
    qualityPreference = normalized;
    qualityAppliedPreference = null;
    qualityRetryCount = 0;
    clearQualityRetry();
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
      boundVideo = video;
      nativeFullscreenAttempted = false;
      video.removeAttribute('playsinline');
      video.removeAttribute('webkit-playsinline');
      video.addEventListener('playing', function () {
        requestNativeFullscreen(video);
        applyQuality();
      }, { once: true });
      video.addEventListener('canplay', function () {
        requestNativeFullscreen(video);
        applyQuality();
      }, { once: true });
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
    qualityRetryCount = 0;
    qualityAppliedPreference = null;
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
