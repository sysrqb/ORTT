package sysrqb.ortt;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private int ORBOT_REQUEST_CODE=9050;
    private String mSocksHost="localhost";
    private int mSocksPort=9050;
    private boolean haveSocksInfo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    protected void onResume() {
        super.onResume();

        if (haveSocksInfo) {
            Intent onionservice = new Intent(this, OnionServiceInfo.class);
            onionservice.putExtra("SOCKS_PROXY_HOST", mSocksHost);
            onionservice.putExtra("SOCKS_PROXY_PORT", mSocksPort);
            startActivity(onionservice);
        }
        System.out.println("Waiting for SOCKS proxy info");
    }

    public void startORTT(View v) {
        String uriOrbot = "org.torproject.android";
        //String orbotStartIntent = "org.torproject.android.intent.action.START";
        String orbotStartIntent = "org.torproject.android.START_TOR";
        String orbotReqPackName = "org.torproject.android.intent.extra.PACKAGE_NAME";
        Intent orbotIntent = new Intent(uriOrbot);
        orbotIntent.setAction(orbotStartIntent);
        PackageManager pm = getPackageManager();
        List availableActivities =
                pm.queryIntentActivities(orbotIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (availableActivities.size() > 0) {
            orbotIntent.putExtra(orbotReqPackName, getPackageName());
            startActivityForResult(orbotIntent, ORBOT_REQUEST_CODE);
            System.out.println("How did we get here?");
        } else {
            System.out.println("There aren't any apps installed that handle the intent(" +
                    uriOrbot + "): " + availableActivities.size());
            OToaster.createToast(getApplicationContext(),
                    "Orbot was not found, please consider installing it.");
        }

        /*try {
            String filename = "test";
            FileOutputStream os;
            os = openFileOutput(filename, Context.MODE_PRIVATE);
            os.write("uname -a".getBytes());
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        /*TorController tc = new TorController();
        if (!tc.connectToControlSocket()) {
            // TODO Show diaglog when this fails
            System.out.println("Connecting to Tor failed!");
        } else {
            System.out.println("Connecting to Tor was successful!");
        }*/
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String EXTRA_SOCKS_PROXY_HOST = "org.torproject.android.intent.extra.SOCKS_PROXY_HOST";
        String EXTRA_SOCKS_PROXY_PORT = "org.torproject.android.intent.extra.SOCKS_PROXY_PORT";
        if (requestCode == ORBOT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                String socksHost = data.getStringExtra(EXTRA_SOCKS_PROXY_HOST);
                if (socksHost == null)
                    socksHost = "127.0.0.1";
                mSocksHost = socksHost;
                mSocksPort = data.getIntExtra(EXTRA_SOCKS_PROXY_PORT, 0);
                haveSocksInfo = true;
                OToaster.createToast(getApplicationContext(), "Orbot told us how to find Tor!");
            } else {
                OToaster.createToast(getApplicationContext(), "Response from Orbot was confused");
            }
        }
    }

    static {
        /* Put this here instead of in the Class file so we can run tests
         * on the Class itself.
         * Okay, we don't have a local tor process working yet, so this isn't
         * needed, we're using Orbot now.
         */
        System.loadLibrary("unixsocketimpl");
    }
}
