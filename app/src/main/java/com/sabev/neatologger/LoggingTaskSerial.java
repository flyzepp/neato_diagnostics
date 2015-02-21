package com.sabev.neatologger;


import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingTaskSerial implements Runnable {

    private final Logger logger;
    private final Context context;
    public LoggingTaskSerial(Context contenxt, Logger logger) {
        this.logger = logger;
        this.context = contenxt;
    }

    @Override
    public void run() {
        UsbSerialPort serialPort;
        try {
            serialPort = openSerialPortConnection();
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "Unable to open serial port connection", ioe);
            sendError(ioe);
            return;
        }
        try {
            processLogStream(serialPort);
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "Error while communicating with Neato", ioe);
            sendError(ioe);
        } finally {
            closePort(serialPort);
        }
    }

    private UsbSerialPort openSerialPortConnection() throws IOException {
        final UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        final List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            final String message = "Unable to find usb serial driver for attached device";
            throw new IOException(message);
        }
        final  UsbSerialDriver driver = availableDrivers.get(0);
        final UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            throw new IOException("Unable to open USB device connection.");
            // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
        }
        final UsbSerialPort port = driver.getPorts().get(0);
        port.open(connection);
        return port;
    }


    private void processLogStream(UsbSerialPort port) throws IOException {
        while (true) {
            //todo process stream and save it to a file
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
        }
    }

    private void closePort(UsbSerialPort port) {
        try {
            port.close();
        } catch (IOException ioe) {
           logger.log(Level.WARNING, "Unable to close port", ioe);
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

