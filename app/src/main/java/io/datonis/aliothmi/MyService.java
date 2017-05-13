package io.datonis.aliothmi;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datonis.aliothmi.adapter.BaseAdapter;
import io.datonis.aliothmi.adapter.OPCUAAdapter;

/**
 * Created by mayank on 7/5/17.
 */

public class MyService extends Service {
    private static Logger logger = LoggerFactory.getLogger(OPCUAAdapter.class);
    Handler serviceHandler;
    Thread dataReaderThread;
    DataReader dataReader;
    // Sets an ID for the notification
    int mNotificationId = 1001;
    NotificationManager mNotifyMgr;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        serviceHandler = new Handler(this.getMainLooper());
        Toast.makeText(this,"Service for Aliot HMI created.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this,"Service for Aliot HMI started.", Toast.LENGTH_SHORT).show();
        if(dataReaderThread == null) {
            dataReader = new DataReader(startId);
            dataReaderThread = new Thread(dataReader);
            dataReaderThread.start();
            Toast.makeText(this,"Data reader thread inside service for Aliot HMI started.", Toast.LENGTH_SHORT).show();
            PendingIntent showPendingIntent =
                    PendingIntent.getActivity(
                            this,
                            0,
                            new Intent(this, MainActivity.class),
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            PendingIntent stopPendingIntent =
                    PendingIntent.getActivity(
                            this,
                            0,
                            new Intent(this, NotificationHandlerActivity.class),
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("Aliot HMI")
                            .setContentText("Aliot data reader service is running.")
                            .setAutoCancel(false)
                            .setOngoing(true)
                            .setTicker("Starting Aliot reader service.")
                            .addAction(R.drawable.ic_stop, "Stop Service", stopPendingIntent);
            //Set the Notification's Click Behavior
            mBuilder.setContentIntent(showPendingIntent);
            Notification notification = mBuilder.build();
            //Issue the Notification
            //Gets an instance of the NotificationManager service
            mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            // Builds the notification and issues it.
            mNotifyMgr.notify(mNotificationId, notification);


        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Service fot Aliot HMI stopped.", Toast.LENGTH_LONG).show();
        mNotifyMgr.cancel(mNotificationId);
        if(dataReaderThread != null && (dataReaderThread.isInterrupted() || dataReaderThread.isAlive())) {
            dataReader.stopThread();
        }
    }

    final class DataReader implements Runnable {
        int service_id;
        BaseAdapter adapter;
        DataHandler dataHandler;
        SharedPreferences preferences;
        SharedPreferences.Editor editor;
        String machineStatusTag;
        String reasonCodeTag;
        boolean stopService = false;

        DataReader(int service_id) {
            this.service_id = service_id;
            preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            editor = preferences.edit();
            dataHandler = new DataHandler(preferences);
            adapter = dataHandler.getAdapter();
            machineStatusTag = dataHandler.getMachineStatusTag();
            reasonCodeTag = dataHandler.getReasonCodeTag();
        }

        public void stopThread() {
            stopService = true;
        }

        @Override
        public void run() {
            if (adapter == null) {
                logger.info("No Adapter Found to perform service task. Killing service.");
                stopSelf(service_id);
                return;
            }
            adapter.connect();
            String[] tags = {machineStatusTag, reasonCodeTag};
            while (!stopService) {
                try {
                    Object[] tagValues = adapter.readTagValues(tags);
                    dataHandler.setCurrentMachineStatus(tagValues[0]);
                    dataHandler.setCurrentReasonCode((int) tagValues[1]);
                    Thread.currentThread().sleep(dataHandler.getDataSyncInterval());
                } catch (InterruptedException e) {
                    logger.error("Unable to sleep the service for 1 sec. " + e);
                    try {
                        Thread.currentThread().sleep(dataHandler.getDataSyncInterval());
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                } catch (Exception e) {
                    logger.error("Unable to read tag values. " + e);
                    try {
                        Thread.currentThread().sleep(dataHandler.getDataSyncInterval());
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
            adapter.shutdown();
        }
    }
}
