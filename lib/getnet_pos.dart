import 'package:flutter/services.dart';

class GetnetPos {
  static const MethodChannel _channel = const MethodChannel('getnet_pos');

  /// Print a list of strings
  static void print(List<String> list) async =>
      await _channel.invokeMethod('print', {'list': list});
}
