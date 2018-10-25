# com-tillerstack-cordova-plugin-background

Plugin for the [Cordova][cordova] framework to perform regular wakeup to allow short background execution.

Most mobile operating systems are multitasking capable, but most apps dont need to run while in background and not present for the user. Therefore they pause the app in background mode and resume the app before switching to foreground mode.
The system keeps all network connections open while in background, but does not deliver the data until the app resumes.

Use the plugin by your own risk!

## Supported Platforms

- **Android/**

## Installation

The plugin can be installed via [Cordova-CLI][cli] and is publicly available on [NPM][npm].

Execute from the projects root folder:

    $ cordova plugin add com-tillerstack-cordova-plugin-background

Or install a specific version:

    $ cordova plugin add com-tillerstack-cordova-plugin-background@VERSION

Or install the latest head version:

    $ cordova plugin add https://github.com/andrefromberlin/com-tillerstack-cordova-plugin-background.git

Or install from local source:

    $ cordova plugin add com-tillerstack-cordova-plugin-background --searchpath <path>

## Usage - ADJUSTALL

The plugin creates the object `cordova.plugins.backgroundMode` and is accessible after the _deviceready_ event has been fired.

```js
document.addEventListener(
  "deviceready",
  function() {
    // cordova.plugins.backgroundMode is now available
  },
  false
);
```

### Enable the background mode

The plugin is not enabled by default. Once it has been enabled the mode becomes active if the app moves to background.

```js
cordova.plugins.backgroundMode.enable();
// or
cordova.plugins.backgroundMode.setEnabled(true);
```

To disable the background mode:

```js
cordova.plugins.backgroundMode.disable();
// or
cordova.plugins.backgroundMode.setEnabled(false);
```

### Check if running in background

Once the plugin has been enabled and the app has entered the background, the background mode becomes active.

```js
cordova.plugins.backgroundMode.isActive(); // => boolean
```

A non-active mode means that the app is in foreground.

### Listen for events

The plugin fires an event each time its status has been changed. These events are `enable`, `disable`, `activate`, `deactivate` and `failure`.

```js
cordova.plugins.backgroundMode.on('EVENT', function);
```

To remove an event listeners:

```js
cordova.plugins.backgroundMode.un('EVENT', function);
```

## Android specifics

### Transit between application states

Android allows to programmatically move from foreground to background or vice versa.

```js
cordova.plugins.backgroundMode.moveToBackground();
// or
cordova.plugins.backgroundMode.moveToForeground();
```

### Unlock and wake-up

A wake-up turns on the screen while unlocking moves the app to foreground even the device is locked.

```js
// Turn screen on
cordova.plugins.backgroundMode.wakeUp();
// Turn screen on and show app even locked
cordova.plugins.backgroundMode.unlock();
```

## Quirks

Any?

**Note:** Something..

## Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request

## License

This software is released under the [MIT License][mit_license].

? 2018 [TillerStack GmbH][tillerstack]

[cordova]: https://cordova.apache.org
[cli]: http://cordova.apache.org/docs/en/edge/guide_cli_index.md.html#The%20Command-line%20Interface
[npm]: ???
[changelog]: CHANGELOG.md
[mit_license]: http://opensource.org/licenses/Apache-2.0
[tillerstack]: http://www.tillerstack.com
