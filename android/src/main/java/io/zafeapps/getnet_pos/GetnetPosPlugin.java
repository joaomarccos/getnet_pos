package io.zafeapps.getnet_pos;

import android.os.IBinder;
import android.os.RemoteException;

import com.getnet.posdigital.PosDigital;
import com.getnet.posdigital.printer.AlignMode;
import com.getnet.posdigital.printer.FontFormat;
import com.getnet.posdigital.printer.IPrinterCallback;

import java.util.List;

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
            public void onError(Exception e) {}

            @Override
            public void onConnected() {
                boolean initiated = PosDigital.getInstance().isInitiated();
                System.out.println("PosDigital is initiated? " + initiated);
            }

            @Override
            public void onDisconnected() {}
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
            if (lines != null && !lines.isEmpty()) {
                try {
                    addTextToPrinter(lines);
                    callPrintMethod(result);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                result.error("Lines are required", null, null);
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
        PosDigital.getInstance().getPrinter().print(new IPrinterCallback() {
            @Override
            public void onSuccess() throws RemoteException {
                result.success("Printed " + android.os.Build.VERSION.RELEASE);
            }

            @Override
            public void onError(int i) throws RemoteException {
                result.error("Error code: " + i, null, null);
            }

            @Override
            public IBinder asBinder() {
                return null;
            }
        });
    }

    /**
     * Add a list of string to the printer buffer
     *
     * @param lines - linest to be printed
     * @throws RemoteException - if printer is not available
     */
    private void addTextToPrinter(List<String> lines) throws RemoteException {
        PosDigital.getInstance().getPrinter().init();
        PosDigital.getInstance().getPrinter().setGray(5);
        PosDigital.getInstance().getPrinter().defineFontFormat(FontFormat.MEDIUM);
        PosDigital.getInstance().getPrinter().addText(AlignMode.CENTER, lines.remove(0));
        for (String line : lines) {
            PosDigital.getInstance().getPrinter().addText(AlignMode.LEFT, line);
        }
    }

    private void getMifare(MethodCall call, Result result) {
        if (call.method.equals("getMifare")) {
            result.success("Mifare");
        }
    }
}
