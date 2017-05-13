package io.datonis.aliothmi;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mayank on 8/4/17.
 */

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    static final String path = "log4j.properties";
    private static Logger logger = LoggerFactory.getLogger(MainActivity.class);
    TextView txtReasonCode;
    TextView txtMachineStatus;
    TextView txtLastIdleTime;
    AlertDialog reasonCodeDialog;
    HomePageHandler homePageHandler;
    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PropertyConfigurator.configure(path);
        logger.info("hi first log");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new HomeFragment()).commit();

        txtReasonCode = (TextView) findViewById(R.id.txtReasonCode);
        txtMachineStatus = (TextView) findViewById(R.id.txtMachineStatus);
        txtLastIdleTime = (TextView) findViewById(R.id.txtLastIdleTime);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        startService(new Intent(this, MyService.class));

        //create dialog for taking reason code input
        AlertDialog.Builder reasonCodeAlertBuilder = new AlertDialog.Builder(this);
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View reasonCodeView = layoutInflater.inflate(R.layout.reason_code_dialog, null);
        reasonCodeAlertBuilder.setView(reasonCodeView);
        final EditText txtInputReasonCode = (EditText) reasonCodeView.findViewById(R.id.input_reason_code);
        reasonCodeAlertBuilder
            .setTitle("Assign Rreason Code")
            .setCancelable(false)
            .setPositiveButton("Assign", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    int enteredReasonCode = Integer.parseInt(txtInputReasonCode.getText().toString());
                    DataHandler dataHandler = new DataHandler(preferences);
                    dataHandler.setLastIdleSlotStartTime(System.currentTimeMillis(), enteredReasonCode);
                }
            });
        reasonCodeDialog = reasonCodeAlertBuilder.create();

    }

    @Override
    protected void onStart() {
        super.onStart();
        homePageHandler = new HomePageHandler(preferences);
        new Thread(homePageHandler).start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        homePageHandler.stop();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private class HomePageHandler implements Runnable {
        DataHandler dataHandler;
        long refreshInterval;
        boolean stop;

        HomePageHandler(SharedPreferences preferences) {
            this.dataHandler =  new DataHandler(preferences);
            this.refreshInterval = dataHandler.getDataSyncInterval();
            this.stop = false;
        }

        public void stop() {
            this.stop = true;
        }

        @Override
        public void run() {
            while (!this.stop) {
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtReasonCode.setText(String.valueOf(dataHandler.getCurrentReasonCode()));
                            boolean currentMachineStatus = dataHandler.getCurrentMachineStatus();
                            txtMachineStatus.setText((currentMachineStatus ? "Running" : "Stopped"));
                            long lastProdcutiveSlotStartTime = dataHandler.getLastProdcutiveSlotStartTime();
                            long lastIdleSlotStartTime = dataHandler.getLastIdleSlotStartTime();
                            long idlePopupTime = dataHandler.getIdlePopupTime();
                            long currentTime = System.currentTimeMillis();
                            long diffInSec = (currentTime - lastIdleSlotStartTime)/1000;
                            if(lastIdleSlotStartTime >= lastProdcutiveSlotStartTime) {
                                txtLastIdleTime.setText(lastIdleSlotStartTime > 0 ? Util.getTimeDiffStr(diffInSec, 7) : "NA");
                            } else {
                                diffInSec = (lastProdcutiveSlotStartTime - lastIdleSlotStartTime)/1000;
                                txtLastIdleTime.setText(lastProdcutiveSlotStartTime > 0 ?
                                        Util.getTimeDiffStr((currentTime-lastProdcutiveSlotStartTime)/1000, 7) : "NA");
                            }
                            if (diffInSec >= idlePopupTime) {
                                if(!reasonCodeDialog.isShowing() && lastIdleSlotStartTime != 0) {
                                    reasonCodeDialog.show();
                                }
                            } else {
                                if (reasonCodeDialog.isShowing()) {
                                    reasonCodeDialog.hide();
                                }
                            }
                        }
                    });
                    Thread.currentThread().sleep(refreshInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
