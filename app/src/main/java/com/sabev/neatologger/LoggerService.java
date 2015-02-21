package com.sabev.neatologger;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.v4.app.NotificationCompat;

import com.sabev.events.IEventHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggerService extends Service {

    public static final BundleFormatter bundleInterface = new BundleFormatter();

    public static final String ACTION_START_LOGGING = "com.sabev.neatologger.ACTION_START_LOGGING";
    public static final String ACTION_STOP_LOGGING = "com.sabev.neatologger.ACTION_STOP_LOGGING";
    public static final String ACTION_CHECK_IS_LOGGING = "com.sabev.neatologger.ACTION_CHECK_STATE";
    public static final String RESULT_RECEIVER = "com.sabev.neatologger.RESULT_RECEIVER";
    public static final int RESULT_RECEIVER_EXCEPTION_CODE = -10;
    public static final int NOTIFICATION_LOGGING = 1;
    private static final String ACTION_CONNECT = "com.sabev.neatologger.ACTION_CONNECT";

    private final Map<String, ActionHandler> actions;
    private LoggerServiceAsyncTasksExecutor asyncTasksTracker;
    private final Logger logger = Logger.getLogger(LoggerService.class.getName());
    private UsbDeviceConnection neatoConnection;


    private interface ActionHandler {
        public Runnable doAction(ResultReceiver resultRecieverFormatter);
    }

    public LoggerService() {
        final Map<String, ActionHandler> tmp = new HashMap<>(3);
        tmp.put(LoggerService.ACTION_START_LOGGING, new StartLoggingAction());
        tmp.put(LoggerService.ACTION_CHECK_IS_LOGGING, new CheckIsLoggingAction());
        tmp.put(LoggerService.ACTION_STOP_LOGGING, new StopLoggingAction());
//        tmp.put(LoggerService.ACTION_CONNECT, new ConnectAction());
        actions = Collections.unmodifiableMap(tmp);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        asyncTasksTracker = new LoggerServiceAsyncTasksExecutor(Executors.newCachedThreadPool());
        stopSelfOnFinishedTask();
        stopForegroundOnFinishedLoggingTask();
    }

    private void stopSelfOnFinishedTask() {
        asyncTasksTracker.getTaskFinishedEvent().addEventHandler(new IEventHandler<String>() {
            @Override
            public void onEvent(String finishedActionTask) {
                if (asyncTasksTracker.getTaskCount() == 0) {
                    stopSelf();
                }
            }
        });
    }

    private void stopForegroundOnFinishedLoggingTask() {
        asyncTasksTracker.getTaskFinishedEvent().addEventHandler(new IEventHandler<String>() {
            @Override
            public void onEvent(String finishedActionTask) {
               if (ACTION_START_LOGGING == finishedActionTask) {
                stopForeground(true);
               }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final ActionHandler handler = actions.get(intent.getAction());
        if (handler == null) {
            throw new IllegalStateException("Unknown Logger Service action: " + intent.getAction());
        }
        if (asyncTasksTracker.get(intent.getAction()) != null) {
            logger.warning("Thread for action " + intent.getAction() + " is still running. Ignoring current request with startId " + startId);
            return START_STICKY;
        }
        final Runnable actionTask = handler.doAction((ResultReceiver)intent.getParcelableExtra(RESULT_RECEIVER));
        logger.info("Action: " + intent.getAction() + " with startId: " + startId + " and flags: " + flags + (actionTask == null ? " finished" : "spawn new thread") + "!");
        if (actionTask != null){
            asyncTasksTracker.submit(intent.getAction(), actionTask);
        } else {
 //           stopSelf(startId);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        asyncTasksTracker.shutdown();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class CheckIsLoggingAction implements ActionHandler {

        @Override
        public Runnable doAction(ResultReceiver resultReciever) {
            resultReciever.send(asyncTasksTracker.get(LoggerService.ACTION_START_LOGGING) == null ? 0 : 1, null);
            return null;
        }
    }

//    private class ConnectAction implements ActionHandler {
//
//        @Override
//        public Runnable doAction(ResultReceiver resultRecieverFormatter) {
//            try {
//
//            } catch (IOException ioe) {
//
//            }
//        }
//
//        private UsbDeviceConnection openNeatoUsbConnection() throws IOException {
//            final UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
//            Map<String, UsbDevice> attachedDevices = manager.getDeviceList();
//            if (attachedDevices.size() == 0) {
//                throw new IOException("Neato not found. 0 USB devices found.");
//            }
//
//
//
//            for (Map.Entry<String, UsbDevice> attachedDevice : attachedDevices.entrySet()) {
//                logger.log(Level.INFO, "Manufacturer name: " + attachedDevice.getValue().getManufacturerName());
//                logger.log(Level.INFO, "Vendor Id: " + attachedDevice.getValue().getVendorId());
//                logger.log(Level.INFO, "Device name (map key): " + attachedDevice.getKey());
//                logger.log(Level.INFO, "Device name (from map value): " + attachedDevice.getValue().getDeviceName());
//                logger.log(Level.INFO, "Device class: " + attachedDevice.getValue().getDeviceClass());
//                logger.log(Level.INFO, "Device subclass: " + attachedDevice.getValue().getDeviceSubclass());
//                logger.log(Level.INFO, "Device id: " + attachedDevice.getValue().getDeviceId());
//                logger.log(Level.INFO, "Device serial number: " + attachedDevice.getValue().getSerialNumber());
//                logger.log(Level.INFO, "Device protocol: " + attachedDevice.getValue().getDeviceProtocol());
//                logger.log(Level.INFO, "Product name: " + attachedDevice.getValue().getProductName());
//                logger.log(Level.INFO, "Product Id subclass: " + attachedDevice.getValue().getProductId());
//            }
//
//            return null;
//        }
//    }

    private class StopLoggingAction implements ActionHandler {

        @Override
        public Runnable doAction(final ResultReceiver resultReceiver) {
            final Future<?> startLoggingTask = asyncTasksTracker.get(LoggerService.ACTION_START_LOGGING);
            if (startLoggingTask == null) {
                return null;
            }
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        startLoggingTask.cancel(true);
                        asyncTasksTracker.waitForTask(startLoggingTask);
                    } catch (Exception e) {
                        resultReceiver.send(RESULT_RECEIVER_EXCEPTION_CODE, bundleInterface.exceptionBundle(e));
                        return;
                    }
                    resultReceiver.send(0, null);
                    stopSelf();
                }
            };
        }
    }

    private class StartLoggingAction implements ActionHandler {

        @Override
        public Runnable doAction(ResultReceiver resultReceiver) {
            resultReceiver.send(0, null);
            startForeground(NOTIFICATION_LOGGING, createLoggingNotification());
            return new LoggingTaskUSB(LoggerService.this, logger);
        }
    }

    private Notification createLoggingNotification() {
        final NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notification_logging)
                        .setContentTitle(getResources().getString(R.string.title_activity_neato_diagnostic))
                        .setContentText(getResources().getString(R.string.notification_logging));
        Intent neatoDiagnostics = new Intent(this, NeatoDiagnostic.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(NeatoDiagnostic.class);
        stackBuilder.addNextIntent(neatoDiagnostics);
        PendingIntent neatoDiagnosticsPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(neatoDiagnosticsPendingIntent);
        return builder.build();
    }

}
