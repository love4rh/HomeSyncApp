package com.tool4us.homesync;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.tool4us.homesync.client.HomeSyncClient;
import com.tool4us.logging.Logs;
import com.tool4us.util.DirectoryChooserDialog;

import java.io.File;

import static com.tool4us.net.common.NetSetting.NS;
import static com.tool4us.util.CommonTool.CT;
import static com.tool4us.homesync.file.Repository.RT;


public class MainActivity extends AppCompatActivity
{
    private static HomeSyncClient   _client = new HomeSyncClient();

    private String _appPath = null;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
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

        _appPath = getModuleDataPath();

        try
        {
            checkAndCreateFolder();

            NS.initialize(getAppSubFolder("temporary"), false, 0);

            _client.start(_appPath);
        }
        catch (Exception xe)
        {
            xe.printStackTrace();
        }

        Logs.info("RootDir:", Environment.getRootDirectory());
        Logs.info("DataDir:", Environment.getDataDirectory());
        Logs.info("ExternalStorage:", Environment.getExternalStorageDirectory());

        Logs.info("PICTURES:", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
        Logs.info("MUSIC:", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC));
        Logs.info("PODCASTS:", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS));
        Logs.info("DIRECTORY_RINGTONES:", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES));
        Logs.info("DIRECTORY_ALARMS:", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS));
        Logs.info("DIRECTORY_NOTIFICATIONS:", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS));
        Logs.info("DIRECTORY_MOVIES:", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES));
        Logs.info("DIRECTORY_DOWNLOADS:", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        Logs.info("DIRECTORY_DCIM:", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));
        Logs.info("DIRECTORY_DOCUMENTS:", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public String getModuleDataPath()
    {
        if( _appPath != null )
            return _appPath;

        String appPath = null;

        try
        {
            PackageManager pm = getPackageManager();
            String pkgName = getPackageName();
            PackageInfo pi = pm.getPackageInfo(pkgName, 0);

            appPath = pi.applicationInfo.dataDir;
            _appPath = appPath;

            Logs.info(pkgName, appPath);
        }
        catch(PackageManager.NameNotFoundException xe)
        {
            xe.printStackTrace();
        }

        return appPath;
    }

    public String getAppSubFolder(String subFolder)
    {
        return CT.concat(getModuleDataPath(), File.separator, subFolder);
    }

    public void checkAndCreateFolder()
    {
        String[] sysFolder = {"syncFolder", "temporary"};

        for(String folder : sysFolder)
        {
            File syncDir = new File(getAppSubFolder(folder));

            if (!syncDir.exists())
            {
                Logs.info(syncDir, "created");

                syncDir.mkdir();
            }
            else
                Logs.info(syncDir, "existed");
        }
    }

    public String wifiAddress()
    {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        WifiInfo wi = wm.getConnectionInfo();
        int ip = wi.getIpAddress();

        if( ip == 0 )
            return "192.168.0.20"; // TODO change to null

        return CT.concat(ip & 0xff, ".", ip >> 8 & 0xff, ".", ip >> 16 & 0xff, ".", ip >> 24 & 0xff);
    }

    public void onClickButton(View v)
    {
        switch(v.getId())
        {
            case R.id.btnConnect: {
                Logs.info("Click Connect");

                if (_client.isConnected()) {
                    Logs.info("already connected.");
                } else {
                    String localIP = wifiAddress();

                    if (localIP != null) {
                        _client.findServer(localIP, 6070);
                    } else {
                        Logs.info("not on a wifi.");
                        // Toast.makeText(getApplicationContext(), getString(R.string.wifiNeeded), Toast.LENGTH_SHORT).show();
                        Snackbar.make(v, R.string.wifiNeeded, Snackbar.LENGTH_SHORT).show();
                    }
                }
            } break;

            case R.id.btnSync: {
                Logs.info("Click Sync");

                if (_client.isConnected()) {
                    _client.compareList();
                } else
                    Logs.info("need to be connected.");

            } break;

            case R.id.btnTest: {
                // Create DirectoryChooserDialog and register a callback
                DirectoryChooserDialog directoryChooserDialog =
                    new DirectoryChooserDialog(MainActivity.this,
                        new DirectoryChooserDialog.ChosenDirectoryListener()
                        {
                            @Override
                            public void onChosenDir(String chosenDir)
                            {
                                Logs.info("Dir Chosen", chosenDir);

                                Toast.makeText(getApplicationContext(), "Chosen directory: " + chosenDir, Toast.LENGTH_LONG).show();
                            }
                        });

                // Toggle new folder button enabling
                directoryChooserDialog.setNewFolderEnabled(true);

                // Load directory chooser dialog for initial 'm_chosenDir' directory.
                // The registered callback will be called upon final directory selection.
                directoryChooserDialog.chooseDirectory(RT.getRootPath()); // "/storage"
                // */
            } break;
        }

    }
}
