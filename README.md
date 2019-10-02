# getnet_pos

A plugin wrapper to use getnet pos hardware sdk on your flutter apps.

**ONLY WORKS ON ANDROID FOR NOW**

## Usage

### Print method

You can pass a list of string as argument. The first element of the list
is the header.

```
import 'package:getnet_pos/getnet_pos.dart';

...

try {
  await GetnetPos.print([
    "Header is the first line",
    "Content line 1",
    "Content line 2",
  ]);
} on PlatformException {
 ..
}
```
