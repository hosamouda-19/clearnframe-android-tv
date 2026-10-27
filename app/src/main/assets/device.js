/* CleanFrame for Android TV. Same FrameDevice API as the Tizen build, backed by the native AndroidBridge. */
(function () {
  'use strict';
  var IDS = {
    stremio: 'com.stremio.one', netflix: 'com.netflix.ninja', disneyplus: 'com.disney.disneyplus',
    youtube: 'com.google.android.youtube.tv', appletv: 'com.apple.atve.androidtv.appletv',
    primevideo: 'com.amazon.amazonvideo.livingroom', spotify: 'com.spotify.tv.android',
    hbomax: 'com.wbd.stream', applemusic: 'com.apple.android.music', tubi: 'com.tubitv'
  };
  var PRIMARY_ORDER = ['stremio', 'netflix', 'disneyplus', 'youtube', 'appletv', 'primevideo', 'spotify', 'hbomax', 'applemusic'];
  function bridge() { return window.AndroidBridge || null; }
  function unavailable(done) {
    done({ message: 'Open CleanFrame on the TV to use this control.', detail: 'The web preview cannot control the TV.' });
  }
  function failure(err) {
    return { message: 'The TV could not open this screen.', detail: err && (err.message || err.name) || 'Try again.' };
  }
  function call(name, arg, done) {
    var b = bridge();
    if (!b) { unavailable(done); return; }
    try {
      var ok = arg === undefined ? b[name]() : b[name](arg);
      if (ok === false) done({ message: 'This app could not be opened.', detail: 'It may not be installed.' }); else done(null);
    } catch (err) { done(failure(err)); }
  }
  function launchApp(id, done) { call('launch', id, done); }
  function launch(brand, done) {
    if (!IDS[brand]) { done({ message: 'This app has no launch target.' }); return; }
    launchApp(IDS[brand], done);
  }
  function art(done) { call('screensaver', undefined, done); }
  function home(done) { call('systemHome', undefined, done); }
  function settings(done) { call('settings', undefined, done); }
  function apps(done) {
    var b = bridge();
    if (!b) { unavailable(done); return; }
    try {
      var list = JSON.parse(b.getApps()), known = {}, result = [];
      Object.keys(IDS).forEach(function (brand) { known[IDS[brand]] = brand; });
      list.forEach(function (a) {
        var brand = known[a.appId] || '';
        result.push({ appId: a.appId, name: a.name, brand: brand, icon: a.icon || '',
          rank: brand && PRIMARY_ORDER.indexOf(brand) >= 0 ? PRIMARY_ORDER.indexOf(brand) : 30 });
      });
      result.sort(function (x, y) { return x.rank - y.rank || x.name.toLowerCase().localeCompare(y.name.toLowerCase()); });
      done(null, result);
    } catch (err) { done(failure(err)); }
  }
  function inputs(done) {
    var b = bridge();
    if (!b) { unavailable(done); return; }
    try { done(null, JSON.parse(b.getInputs())); } catch (err) { done(failure(err)); }
  }
  function status(done) {
    var b = bridge();
    if (!b) { unavailable(done); return; }
    try { done(null, JSON.parse(b.getStatus())); } catch (err) { done(failure(err)); }
  }
  function switchInput(id, done) { call('switchInput', id, done); }
  function openSystem(kind, done) { call('openSetting', kind, done); }
  window.FrameDevice = { status: status, inputs: inputs, switchInput: switchInput, openSystem: openSystem, launch: launch, launchApp: launchApp, art: art, home: home, settings: settings, apps: apps };
}());
