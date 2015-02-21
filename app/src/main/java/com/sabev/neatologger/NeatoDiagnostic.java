package com.sabev.neatologger;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ResultReceiver;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;


public class NeatoDiagnostic extends ActionBarActivity {

    public static String showErrorMessageIntentAction = "com.sabev.neatologger.SHOW_ERROR";
    public static String exception_extra = "com.sabev.neatologger.EXCEPTION_EXTRA";

    Handler updateControlButton = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            getControlButton().setEnabled(msg.arg1 != 0);
            getControlButton().setChecked(msg.arg2 != 0);
        }
    };

    private Message buttonStateMessage(Handler handler, boolean enabled, boolean checked) {
        Message m = handler.obtainMessage();
        m.arg1 = enabled ? 1 : 0;
        m.arg2 = checked ? 1 : 0;
        return m;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_neato_diagnostic);
        getControlButton().setEnabled(false);
        getControlButton().setOnClickListener(new ControlButtonClickHandler());
        onNewIntent(getIntent());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getAction().equals(showErrorMessageIntentAction)) {
            handleException(intent.getBundleExtra(exception_extra), null);
        }
    }

    @Override
    protected void onResume() {
       super.onResume();
       sendCheckIsLoggingIntent();
    }

    private void sendCheckIsLoggingIntent() {
        final Intent checkIsLogging = new Intent(this, LoggerService.class);
        checkIsLogging.setAction(LoggerService.ACTION_CHECK_IS_LOGGING);
        ResultReceiver controlButtonInitialStateSetter = new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (gotException(resultCode)) {
                    updateControlButton.sendMessage(buttonStateMessage(updateControlButton, false, resultCode == 1));
                    handleException(resultData, null);
                    return;
                }
                updateControlButton.sendMessage(buttonStateMessage(updateControlButton, true, resultCode == 1));
            }
        };
        checkIsLogging.putExtra(LoggerService.RESULT_RECEIVER, controlButtonInitialStateSetter);
        startService(checkIsLogging);
    }

    private boolean gotException(int resultCode) {
        return resultCode == LoggerService.RESULT_RECEIVER_EXCEPTION_CODE;
    }

    private void handleException(Bundle resultData, final Runnable codeToExecuteOnDialogDismiss) {
          new AlertDialog
                .Builder(this).setTitle("An error has occured")
                .setMessage(LoggerService.bundleInterface.getException(resultData).getMessage())
                .setPositiveButton(R.string.dialog_ok_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (codeToExecuteOnDialogDismiss != null) {
                            codeToExecuteOnDialogDismiss.run();
                        }
                    }
                })
                .create()
                .show();
    }

    private class ControlButtonClickHandler implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            ToggleButton controlButton = (ToggleButton)v;
            if(controlButton.isChecked()) {
                hack();
                sendControlIntent(LoggerService.ACTION_START_LOGGING, false);
            } else {
                sendControlIntent(LoggerService.ACTION_STOP_LOGGING, true);
            }
        }
    }

    private void hack() {
        final UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        Map<String, UsbDevice> attachedDevices = manager.getDeviceList();
        if (attachedDevices.size() == 0) {
            throw new IOException("Neato not found. 0 USB devices found.");
        }

        for (Map.Entry<String, UsbDevice> attachedDevice : attachedDevices.entrySet()) {
            logger.log(Level.INFO, "Manufacturer name: " + attachedDevice.getValue().getManufacturerName());
            logger.log(Level.INFO, "Vendor Id: " + attachedDevice.getValue().getVendorId());
            logger.log(Level.INFO, "Device name (map key): " + attachedDevice.getKey());
            logger.log(Level.INFO, "Device name (from map value): " + attachedDevice.getValue().getDeviceName());
            logger.log(Level.INFO, "Device class: " + attachedDevice.getValue().getDeviceClass());
            logger.log(Level.INFO, "Device subclass: " + attachedDevice.getValue().getDeviceSubclass());
            logger.log(Level.INFO, "Device id: " + attachedDevice.getValue().getDeviceId());
            logger.log(Level.INFO, "Device serial number: " + attachedDevice.getValue().getSerialNumber());
            logger.log(Level.INFO, "Device protocol: " + attachedDevice.getValue().getDeviceProtocol());
            logger.log(Level.INFO, "Product name: " + attachedDevice.getValue().getProductName());
            logger.log(Level.INFO, "Product Id subclass: " + attachedDevice.getValue().getProductId());
        }

        return null;
    }


    private void sendControlIntent(String action, final boolean buttonStateOnError) {
        final Intent controlLoggingIntent = new Intent(this, LoggerService.class);
        controlLoggingIntent.setAction(action);
        controlLoggingIntent.putExtra(LoggerService.RESULT_RECEIVER, new ResultReceiver(null) {

            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (gotException(resultCode)) {
                    handleException(resultData, null);
                    updateControlButton.sendMessage(buttonStateMessage(updateControlButton, true, buttonStateOnError));
                }
                updateControlButton.sendMessage(buttonStateMessage(updateControlButton, true, !buttonStateOnError));
            }
        });
        startService(controlLoggingIntent);
        getControlButton().setEnabled(false);
    }

    private ToggleButton getControlButton() {
        return (ToggleButton)findViewById(R.id.control_button);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_neato_diagnostic, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
