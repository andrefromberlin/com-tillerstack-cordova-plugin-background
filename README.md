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

## Usage

The plugin creates the object `navigator.background` and is accessible after the _deviceready_ event has been fired.

```js
document.addEventListener(
  "deviceready",
  function() {
    // navigator.background is now available
  },
  false
);
```

### Register for background wakeups

You need to register and unregister for being woken up regularly while the app is in background. During Registration you are required to provide 
- a callback function that is being called whenever the plugin detects app lifecycle changes you might want to react on and set wakeup alarms or such
- a callback function that is being called whenever the plugin detects an error while trying to perform duties on background handling

To register:

```js
if (navigator.background) {
    navigator.background.register(cbOnPluginMessage, cbOnPluginErrorOccurred);
}
```

To unregister:

```js
if (navigator.background) {
    navigator.background.unregister(cbOnSuccess, cbOnError);
}
```

### Receive a plugin message

Once the plugin detects app lifecycle changes it can be used to set a regular wakeup call for the app in order to allow a regular small app execution task.

```js
function cbOnPluginMessage(result) {
    if (result && result.state !== backgroundEnvironmentStates.UNDEFINED) {
      // you could, for example..
      // - broadcast the environment state change in your entire app
      // $rootScope.$broadcast(result.state);

      // ... and / or
      // - depending on the state, set or cancel ALARMs
      if (
        result.state === backgroundEnvironmentStates.ACTIVITY_PAUSED ||
        result.state === backgroundEnvironmentStates.ALARM_WAKEUP_ONCE
      ) {
        setAlarm();
      } else if (
        result.state === backgroundEnvironmentStates.ACTIVITY_RESUMED
      ) {
        cancelAlarm();
      }
    } else if (result !== 'OK') {
      console.log('Plugin message was not "OK": ' + result);
    }
  }
```

### Set and cancel wakeup alarms

The plugin can be used to set an OS alarm to be woken up on a specific time regularly. Depending on the Android Version this is internally either set up once as a recurring alarm, or set up as a starting single alarm which is reset every time it got triggered.

When creating an alarm you have to specify the amount of seconds you want to be woken up after once the alarm is set. As the transfer object is a parameters array, yoh need to declare the seconds as the first (and only) array object.

To set an alarm:

```js
if (navigator.background) {
      navigator.background.setAlarm(cbOnSuccess, cbOnError, [seconds]);
}
```

To cancel an alarm:

```js
if (navigator.background) {
      navigator.background.cancelAlarm(cbOnSuccess, cbOnError);
}
```

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
[mit_license]: https://opensource.org/licenses/MIT
[tillerstack]: http://www.tillerstack.com
