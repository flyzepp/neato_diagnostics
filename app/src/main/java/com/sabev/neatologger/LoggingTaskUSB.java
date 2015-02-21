package com.sabev.neatologger;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingTaskUSB implements Runnable {

    private final Logger logger;
    private final Context context;
    public LoggingTaskUSB(Context contenxt, Logger logger) {
        this.logger = logger;
        this.context = contenxt;
    }

    @Override
    public void run() {
//        UsbDeviceConnection neatoConnection;
//        try {
//            neatoConnection = openNeatoUsbConnection();
//        } catch (IOException ioe) {
//            logger.log(Level.SEVERE, "Unable to open Neato USB connection", ioe);
//            sendError(ioe);
//            return;
//        }
//        try {
//            processLogStream(neatoConnection);
//        } catch (IOException ioe) {
//            logger.log(Level.SEVERE, "Error while communicating with Neato", ioe);
//            sendError(ioe);
//        } finally {
//            neatoConnection.close();
//        }
    }




    private void processLogStream(UsbDeviceConnection neatoConnection) throws IOException {
        while (true) {
            //todo process stream and save it to a file
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
        }
    }

    private void sendError(Exception e) {
        Intent i = new Intent(context, NeatoDiagnostic.class);
        i.setAction(NeatoDiagnostic.showErrorMessageIntentAction);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(NeatoDiagnostic.exception_extra, LoggerService.bundleInterface.exceptionBundle(e));
        context.startActivity(i);
    }



//
//// Open a connection to the first available driver.
//
//
//        while(true) {
//
//
//            try {
//                port.setBaudRate(115200);
//                byte buffer[] = new byte[16];
//                int numBytesRead = port.read(buffer, 1000);
//                Log.d(TAG, "Read " + numBytesRead + " bytes.");
//            } catch (IOException e) {
//                // Deal with error.
//            } finally {
//                port.close();
//            }
//
//
//            try {
//                Thread.sleep(10);
//                logger.info("doing work");
//            } catch (InterruptedException ie) {
//                return;
//            }
//            if (Thread.currentThread().isInterrupted()) {
//                return;
//            }
//        }
}

