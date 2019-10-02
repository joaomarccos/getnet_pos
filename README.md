# getnet_pos

A plugin wrapper to use getnet pos hardware sdk on your flutter apps.

**ONLY WORKS ON ANDROID FOR NOW**

## Usage

### Configure your project

Change minSdkVersion to 22 on your app
```
 defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId "com.example.test_getnet_pos_package"
        minSdkVersion 22
        targetSdkVersion 28
        versionCode flutterVersionCode.toInteger()
        versionName flutterVersionName
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
    }
``` 

In android/app folder add the getnet sdk dependency (must be an .arr file)

```
.
├── android
│   ├── app
│   │   ├── build.gradle
│   │   ├── libs
│   │   │   └── libposdigital-1.4.0-2-release.aar

```


### Print method

You can pass a list of string as argument. The first element of the list
is the header.

```
import 'package:getnet_pos/getnet_pos.dart';

...

try {
  GetnetPos.print([
    "Header is the first line",
    "Content line 1",
    "Content line 2",
  ]);
} on PlatformException {
 ..
}
```
