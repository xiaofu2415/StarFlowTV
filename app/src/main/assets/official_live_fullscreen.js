(function () {
  'use strict';

  var API_NAME = '__starflowOfficialFullscreen';
  var ROOT_CLASS = 'starflow-official-fullscreen';
  var STYLE_ID = 'starflow-official-fullscreen-style';

  if (window[API_NAME]) {
    window[API_NAME].enter();
    return;
  }

  var observer = null;
  var retryTimer = null;
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
      }, { once: true });
      video.addEventListener('canplay', function () {
        requestNativeFullscreen(video);
      }, { once: true });
    }
    if (video && !video.paused) requestNativeFullscreen(video);
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
      observer = new MutationObserver(promotePlayer);
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
    exit: exit
  };
  enter();
}());
