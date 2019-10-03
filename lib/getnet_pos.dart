import 'package:flutter/services.dart';

class GetnetPos {
  static const MethodChannel _channel = const MethodChannel('getnet_pos');

  /// Print a list of strings.
  /// Uses the qrCodePattern to match the qrCode. If matches the qrcode is printed.
  /// Uses the barcodePattern to match the barcode. If matches the barcode is printed.
  static void print(
    List<String> list, {
    String qrCodePattern = '(\\d{44}\\|.*\$)',
    String barcodePattern = '^\\d*\$',
  }) async =>
      await _channel.invokeMethod('print', {
        'list': list,
        'qrCodePattern': qrCodePattern,
        'barcodePattern': barcodePattern,
      });
}
