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
  var nativeFullscreenRequested = false;
  var boundVideo = null;

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
      'html.' + ROOT_CLASS + ' #player{position:fixed!important;inset:0!important;left:0!important;top:0!important;width:100vw!important;height:100vh!important;max-width:none!important;margin:0!important;padding:0!important;z-index:2147483646!important;background:#000!important;}',
      'html.' + ROOT_CLASS + ' #player,html.' + ROOT_CLASS + ' #player *{box-sizing:border-box!important;max-width:none!important;}',
      'html.' + ROOT_CLASS + ' #player>div,html.' + ROOT_CLASS + ' #html5Player_live,html.' + ROOT_CLASS + ' #html5Player,html.' + ROOT_CLASS + ' #html5VideoBack,html.' + ROOT_CLASS + ' #html5ControlDiv,html.' + ROOT_CLASS + ' #h5canvas_player{width:100%!important;height:100%!important;margin:0!important;}',
      'html.' + ROOT_CLASS + ' #player video,html.' + ROOT_CLASS + ' #player canvas,html.' + ROOT_CLASS + ' #player object,html.' + ROOT_CLASS + ' #player embed{position:absolute!important;inset:0!important;width:100%!important;height:100%!important;object-fit:contain!important;background:#000!important;}'
    ].join('');
    document.head.appendChild(style);
    return style;
  }

  function requestNativeFullscreen(video) {
    if (!video || nativeFullscreenRequested
        || document.fullscreenElement || document.webkitFullscreenElement) return;
    var request = video.requestFullscreen || video.webkitRequestFullscreen;
    if (typeof request !== 'function') return;
    nativeFullscreenRequested = true;
    try {
      var result = request.call(video);
      if (result && typeof result.catch === 'function') {
        result.catch(function () {
          nativeFullscreenRequested = false;
        });
      }
    } catch (ignored) {
      nativeFullscreenRequested = false;
    }
  }

  function promotePlayer() {
    ensureStyle();
    document.documentElement.classList.add(ROOT_CLASS);
    var player = findPlayer();
    if (!player) return false;
    player.setAttribute('data-starflow-fullscreen', 'true');

    var video = player.querySelector('video');
    if (video && video !== boundVideo) {
      boundVideo = video;
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
    ensureStyle();
    promotePlayer();
    if (!observer && typeof MutationObserver === 'function') {
      observer = new MutationObserver(promotePlayer);
      observer.observe(document.body || document.documentElement, {
        childList: true,
        subtree: true
      });
    }
    if (!retryTimer) retryTimer = setInterval(promotePlayer, 500);
  }

  function exit() {
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
    nativeFullscreenRequested = false;
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
