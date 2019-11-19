package io.zafeapps.getnet_pos;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.RemoteException;

import com.getnet.posdigital.PosDigital;
import com.getnet.posdigital.camera.ICameraCallback;
import com.getnet.posdigital.mifare.IMifareCallback;
import com.getnet.posdigital.printer.AlignMode;
import com.getnet.posdigital.printer.FontFormat;
import com.getnet.posdigital.printer.IPrinterCallback;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * GetnetPosPlugin
 */
public class GetnetPosPlugin implements MethodCallHandler {

    private static final Logger LOGGER = Logger.getLogger(GetnetPosPlugin.class.getName());
    private static final String QR_CODE_PATTERN = "qrCodePattern";
    private static final String BARCODE_PATTERN = "barcodePattern";
    private static final String PRINT_BARCODE = "printBarcode";
    private static final String LIST = "list";
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        context = registrar.context();
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "getnet_pos");
        channel.setMethodCallHandler(new GetnetPosPlugin());
    }

    /**
     * Init the PosDigital Hardware SDK
     */
    private static void doWhenInitialized(final Callback callback) {
        try {
            if (!PosDigital.getInstance().isInitiated()) {
                registerPosDigital(callback);
            } else {
                callback.performAction();
            }
        } catch (RuntimeException e) {
            registerPosDigital(callback);
        }
    }

    private static void registerPosDigital(final Callback callback) {
        PosDigital.register(context, new PosDigital.BindCallback() {
            @Override
            public void onError(Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                callback.onError(e.getMessage());
            }

            @Override
            public void onConnected() {
                try {
                    callback.performAction();
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }

            @Override
            public void onDisconnected() {
                LOGGER.info("PosDigital service getnet disconnected");
            }
        });
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        print(call, result);
        getMifare(call, result, 10);
        scanner(call, result);
        checkService(call, result);
    }

    /**
     * Check if the service is initialized
     *
     * @param call   - method call
     * @param result - result callback
     */
    private void checkService(MethodCall call, Result result) {
        if (call.method.equals("check")) {
            boolean initiated = false;
            try {
                initiated = PosDigital.getInstance().isInitiated();
                PosDigital.getInstance().getBeeper().success();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
            }
            result.success(initiated);
        }
    }

    /**
     * Print
     *
     * @param call   - method call
     * @param result - result callback
     */
    private void print(final MethodCall call, final Result result) {
        if (call.method.equals("print")) {
            doWhenInitialized(new Callback() {
                @Override
                public void performAction() {
                    List<String> lines = call.argument(LIST);
                    String qrCodePattern = call.argument(QR_CODE_PATTERN);
                    String barCodePattern = call.argument(BARCODE_PATTERN);
                    boolean printBarcode = call.argument(PRINT_BARCODE);
                    if (lines != null && !lines.isEmpty()) {
                        try {
                            addTextToPrinter(lines, qrCodePattern, barCodePattern, printBarcode);
                            callPrintMethod(result);
                        } catch (Exception e) {
                            result.error("Error on print", e.getMessage(), e);
                        }
                    } else {
                        result.error("Arguments are missed [list, qrCodePattern, barcodePattern]", null, null);
                    }
                }

                @Override
                public void onError(String message) {
                    result.error("print", message, null);
                }
            });
        }
    }

    /**
     * Invoke print method and set status of operation on result callback
     *
     * @param result - result for handler the status
     * @throws RemoteException - if the printer is not available
     */
    private void callPrintMethod(final Result result) throws RemoteException {
        PosDigital.getInstance().getPrinter().print(new IPrinterCallback.Stub() {
            @Override
            public void onSuccess() {
                result.success("Printed.");
            }

            @Override
            public void onError(int i) {
                result.error("Error code: " + i, null, null);
            }
        });
    }

    /**
     * Add a list of string to the printer buffer
     *
     * @param lines - linest to be printed
     * @throws RemoteException - if printer is not available
     */
    private void addTextToPrinter(List<String> lines, String qrCodePattern, String barcodePattern, boolean printBarCode) throws RemoteException {
        PosDigital.getInstance().getPrinter().init();
        PosDigital.getInstance().getPrinter().setGray(5);
        PosDigital.getInstance().getPrinter().defineFontFormat(FontFormat.SMALL);
        PosDigital.getInstance().getPrinter().addText(AlignMode.CENTER, lines.remove(0));
        Pattern patternQrCode = Pattern.compile(qrCodePattern, Pattern.MULTILINE);
        Pattern patternBarCode = Pattern.compile(barcodePattern, Pattern.MULTILINE);
        for (String text : lines) {
            for (String line : text.split("\n")) {
                Matcher qrcodeMatcher = patternQrCode.matcher(line);
                Matcher barcodeMather = patternBarCode.matcher(line);
                if (qrcodeMatcher.find()) {
                    PosDigital.getInstance().getPrinter().addQrCode(AlignMode.CENTER, 360, line);
                } else if (printBarCode && barcodeMather.find()) {
                    PosDigital.getInstance().getPrinter().addText(AlignMode.CENTER, line);
                    PosDigital.getInstance().getPrinter().addBarCode(AlignMode.CENTER, line.trim());
                    PosDigital.getInstance().getPrinter().addText(AlignMode.CENTER, "");
                } else {
                    PosDigital.getInstance().getPrinter().addText(AlignMode.LEFT, line);
                }
            }
        }
    }

    /**
     * Try to read a next card
     *
     * @param call        - method call
     * @param result      - result callback
     * @param remainTries - number of tries
     */
    private void getMifare(final MethodCall call, final Result result, final int remainTries) {
        if (call.method.equals("getMifare")) {
            doWhenInitialized(new Callback() {
                @Override
                public void performAction() {
                    try {
                        PosDigital.getInstance().getMifare().searchCard(new IMifareCallback.Stub() {
                            @Override
                            public void onCard(int i) throws RemoteException {
                                result.success(PosDigital.getInstance().getMifare().getCardSerialNo(i));
                                PosDigital.getInstance().getMifare().halt();
                            }

                            @Override
                            public void onError(String s) throws RemoteException {
                                if (remainTries > 0) {
                                    getMifare(call, result, remainTries - 1);
                                } else {
                                    result.error("Error on Mifare", s, null);
                                }
                            }
                        });
                    } catch (RemoteException e) {
                        result.error("Error", e.getMessage(), null);
                    }
                }

                @Override
                public void onError(String message) {
                    result.error("getMifare", message, null);
                }
            });
        }
    }

    /**
     * Try read some barcode/qrcode using the back camera
     *
     * @param call   - method call
     * @param result - result callback
     */
    private void scanner(final MethodCall call, final Result result) {
        if (call.method.equals("scanner")) {
            doWhenInitialized(new Callback() {
                @Override
                public void performAction() {
                    try {
                        PosDigital.getInstance().getCamera().readBack(5000, new ICameraCallback.Stub() {
                            @Override
                            public void onSuccess(String s) throws RemoteException {
                                LOGGER.info("Info obtained from scanner: " + s);
                                result.success(s);
                            }

                            @Override
                            public void onTimeout() throws RemoteException {
                                result.error("Timeout Error", "Timeout expired", null);
                            }

                            @Override
                            public void onCancel() throws RemoteException {
                                result.error("Cancelled", "Cancelled by the user", null);
                            }

                            @Override
                            public void onError(String s) throws RemoteException {
                                result.error("Error on scanner", s, null);
                            }
                        });
                    } catch (RemoteException e) {
                        result.error("Error", e.getMessage(), null);
                    }
                }

                @Override
                public void onError(String message) {
                    result.error("scanner", message, null);
                }
            });
        }
    }

    private interface Callback {
        void performAction();

        void onError(String message);
    }

}
