package io.zafeapps.getnet_pos;

import android.os.IBinder;
import android.os.RemoteException;

import com.getnet.posdigital.PosDigital;
import com.getnet.posdigital.mifare.IMifareCallback;
import com.getnet.posdigital.mifare.MifareStatus;
import com.getnet.posdigital.printer.AlignMode;
import com.getnet.posdigital.printer.FontFormat;
import com.getnet.posdigital.printer.IPrinterCallback;

import java.util.List;
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

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        initPosDigital(registrar);
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "getnet_pos");
        channel.setMethodCallHandler(new GetnetPosPlugin());
    }

    /**
     * Init the PosDigital Hardware SDK
     *
     * @param registrar - Registrar instance to get application context
     */
    private static void initPosDigital(Registrar registrar) {
        PosDigital.register(registrar.context(), new PosDigital.BindCallback() {
            @Override
            public void onError(Exception e) {
            }

            @Override
            public void onConnected() {
                boolean initiated = PosDigital.getInstance().isInitiated();
                System.out.println("PosDigital is initiated? " + initiated);
            }

            @Override
            public void onDisconnected() {
            }
        });
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        getMifare(call, result);
        print(call, result);
    }

    private void print(MethodCall call, final Result result) {
        if (call.method.equals("print")) {
            List<String> lines = call.argument("list");
            String qrCodePattern = call.argument("qrCodePattern");
            String barCodePattern = call.argument("barcodePattern");
            boolean printBarcode = call.argument("printBarcode");
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
            try {
                PosDigital.getInstance().getMifare().searchCard(new IMifareCallback.Stub() {
                    @Override
                    public void onCard(int i) throws RemoteException {
                        int activate = PosDigital.getInstance().getMifare().activate(i);
                        if (activate == MifareStatus.SUCCESS) {
                            result.success(PosDigital.getInstance().getMifare().getCardSerialNo(i));
                        } else {
                            result.error("Error: line 145", "Error on Mifare. Code: "+i, null);
                        }
                        PosDigital.getInstance().getMifare().halt();
                    }

                    @Override
                    public void onError(String s) throws RemoteException {
                        result.error("Error: line 152", s, null);
                    }
                });
            } catch (RemoteException e) {
                result.error("Error: line 156", e.getMessage(), null);
            }
        }
    }
}
