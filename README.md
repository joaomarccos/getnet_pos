# getnet_pos

A plugin wrapper to use getnet pos hardware sdk in your flutter apps.

**ONLY WORKS ON ANDROID FOR NOW**

## Usage

### Configure your project

Change **minSdkVersion to 22** in your app
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

In android/app folder **add the getnet sdk dependency** (must have .aar extension)

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
is the header. Print barcode and QrCode if find a text that match the given 
patterns.

```
import 'package:getnet_pos/getnet_pos.dart';

...

try {
  GetnetPos.print(
    [
     "Header is the first line",
     "Content line 1",
     "Content line 2",
    ],
    printBarcode = false, //default is true
    barcodePattern = '^\\d{1,}.\$', //by default
    qrCodePattern = '^\\d{1,}.\$', //by default
  );
} on PlatformException {
 ..
}
```
