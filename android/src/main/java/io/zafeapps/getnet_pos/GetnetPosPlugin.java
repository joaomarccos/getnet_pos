package io.zafeapps.getnet_pos;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.RemoteException;

import com.getnet.posdigital.PosDigital;
import com.getnet.posdigital.camera.ICameraCallback;
import com.getnet.posdigital.mifare.IMifareActivateCallback;
import com.getnet.posdigital.mifare.IMifareCallback;
import com.getnet.posdigital.mifare.MifareStatus;
import com.getnet.posdigital.printer.AlignMode;
import com.getnet.posdigital.printer.FontFormat;
import com.getnet.posdigital.printer.IPrinterCallback;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
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
    private static void initPosDigital(final Callable<Void> callback) {
        if (PosDigital.getInstance().isInitiated()) PosDigital.unregister(context);
        PosDigital.register(context, new PosDigital.BindCallback() {
            @Override
            public void onError(Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }

            @Override
            public void onConnected() {
                try {
                    callback.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("PosDigital is initiated? " + PosDigital.getInstance().isInitiated());
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
        getMifare(call, result);
        scanner(call, result);
    }

    private void print(final MethodCall call, final Result result) {
        if (call.method.equals("print")) {
            initPosDigital(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
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
                    return null;
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

    private void getMifare(final MethodCall call, final Result result) {
        if (call.method.equals("getMifare")) {
            initPosDigital(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    try {
                        PosDigital.getInstance().getMifare().searchCard(new IMifareCallback.Stub() {
                            @Override
                            public void onCard(int i) throws RemoteException {
                                int activate = PosDigital.getInstance().getMifare().activate(i);
                                if (activate == MifareStatus.SUCCESS) {
                                    result.success(PosDigital.getInstance().getMifare().getCardSerialNo(i));
                                } else {
                                    result.error("Error on card activation", "Error on Mifare. Code: " + i, null);
                                }
                                PosDigital.getInstance().getMifare().halt();
                            }

                            @Override
                            public void onError(String s) throws RemoteException {
                                result.error("Error on Mifare", s, null);
                            }
                        });
                    } catch (RemoteException e) {
                        result.error("Error", e.getMessage(), null);
                    }
                    return null;
                }
            });
        }
    }

    private void scanner(final MethodCall call, final Result result) {
        if (call.method.equals("scanner")) {
            initPosDigital(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    try {
                        PosDigital.getInstance().getCamera().readBack(5000, new ICameraCallback.Stub() {
                            @Override
                            public void onSuccess(String s) throws RemoteException {
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
                    return null;
                }
            });
        }
    }

}
