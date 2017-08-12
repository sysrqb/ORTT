package sysrqb.ortt;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
//import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.IllegalBlockingModeException;
// API level 26
//import java.time.Instant;
import java.util.Calendar;
import java.util.Vector;

public class OnionServiceInfo extends AppCompatActivity {
    private String mSocksHost;
    private int mSocksPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onion_service_info);

        mSocksHost = getIntent().getStringExtra("SOCKS_PROXY_HOST");
        mSocksPort = getIntent().getIntExtra("SOCKS_PROXY_PORT", 0);
    }

    public void startTimer(View v) {
        String onionAddr;
        String onionPortRaw;

        EditText editTextOnion = (EditText) findViewById(R.id.onion_service_addr_field);
        onionAddr = editTextOnion.getText().toString();
        System.out.println("Received '" + onionAddr + "' from input");

        editTextOnion = (EditText) findViewById(R.id.onion_service_port_field);
        onionPortRaw = editTextOnion.getText().toString();
        System.out.println("Received '" + onionPortRaw + "' from input");
        OToaster.createToast(getApplicationContext(), "Let's go!");

        new SocketActivities().execute(onionAddr, onionPortRaw);
    }

    private class SocketActivities extends AsyncTask<String, String, Long[]> {
        //protected Instant[] doInBackground(String... inputs) {
        protected Long[] doInBackground(String... inputs) {
            //Instant begin, half, roundtrip;
            Long torconn, begin, half, roundtrip;
            int count = inputs.length;
            if (count != 2) {
                System.out.println("Got " + count + " inputs!");
                return null;
            }
            String onionAddr = inputs[0];
            String onionPortRaw = inputs[1];

            if (onionPortRaw == null) {
                System.out.println("Onion port number is null!");
                return null;
            }
            int onionPort = Integer.parseInt(onionPortRaw);

            if (onionPort < 0 || onionPort > 65535) {
                onionPort = 0;
            }

            System.out.println("Creating client socket");
            ServerSocket ss = getSocketAddress(onionPort);
            /* TODO Correctly handle these exceptions */
            if (ss == null) {
                System.out.println("ServerSocket is null. Returning.");
                return null;
            }

            System.out.println("Creating onion service socket");

            torconn = Calendar.getInstance().getTimeInMillis();
            Socket onionSock = createSocksConnection(mSocksHost, mSocksPort, onionAddr, onionPort);
            if (onionSock == null) {
                System.out.println("Creating client socket failed. Returning.");
                try {
                    ss.close();
                } catch (IOException e) {
                }
                return null;
            }

            //begin = Instant.now();
            System.out.println("Saving first time point");
            publishProgress("Ready...Set...Go!");
            begin = Calendar.getInstance().getTimeInMillis();
            String resStr;
            resStr = sendPing(onionSock);
            if (resStr != null) {
                System.out.println("sendPing returned false. Returning.");
                publishProgress(resStr);
                try {
                    ss.close();
                    onionSock.close();
                } catch (IOException e) {
                }
                return null;
            }
            publishProgress("Ping!");
            resStr = waitForPing(ss);
            if (resStr != null) {
                System.out.println("waitForPing returned false. Returning.");
                publishProgress(resStr);
                try {
                    ss.close();
                    onionSock.close();
                } catch (IOException e) {
                }
                return null;
            }
            //half = Instant.now();
            System.out.println("Saving second time point");
            half = Calendar.getInstance().getTimeInMillis();
            publishProgress("Pong!");
            resStr = waitForPong(onionSock);
            if (resStr != null) {
                System.out.println("waitForPong returned false. Returning.");
                publishProgress(resStr);
                try {
                    ss.close();
                    onionSock.close();
                } catch (IOException e) {
                }
                return null;
            }
            //roundtrip = Instant.now();
            System.out.println("Saving last time point");
            roundtrip = Calendar.getInstance().getTimeInMillis();
            //return new Instant[]{begin, half, roundtrip};
            return new Long[]{begin, half, roundtrip};
        }

        protected void onProgressUpdate(String... values) {
            if (values == null || values.length != 1)
                return;
            OToaster.createToast(getApplicationContext(), values[0]);
        }

        //protected void onPostExecute(Instant[] instants) {
        protected void onPostExecute(Long[] millis) {
            //if (instants.length != 3) {
            if (millis == null || millis.length != 3) {
                return;
            }
            /*Instant begin = instants[0];
            Instant half = instants[1];
            Instant roundtrip = instants[2];

            long halfTimeSeconds = half.getEpochSecond() - begin.getEpochSecond();
            int halfTimeNano = half.getNano() - begin.getNano();

            long secondHalfTimeSeconds = roundtrip.getEpochSecond() - half.getEpochSecond();
            int secondHalfTimeNano = roundtrip.getNano() - half.getNano();

            long totalTimeSeconds = roundtrip.getEpochSecond() - begin.getEpochSecond();
            int totalTimeNano = roundtrip.getNano() - begin.getNano();*/

            Long begin = millis[0];
            Long half = millis[1];
            Long roundtrip = millis[2];

            long halfTimeMilliSeconds = half - begin;

            long secondHalfTimeMilliSeconds = roundtrip - half;

            long totalTimeMilliSeconds = roundtrip - begin;

            System.out.println("Complete RTT: " + totalTimeMilliSeconds + " seconds");
            System.out.println("First half: " + halfTimeMilliSeconds + " seconds");
            System.out.println("Second half: " + secondHalfTimeMilliSeconds + " seconds");
        }

        private ServerSocket getSocketAddress(int port) {
            InetSocketAddress addr = new InetSocketAddress("127.0.0.1", port);
            try {
                ServerSocket sock = new ServerSocket();
                sock.bind(addr);
                return sock;
            } catch (SecurityException e) {
                publishProgress("SecurityException during bind()");
                System.out.println("Received SecurityException from getSocketAddress()");
                e.printStackTrace();
                return null;
            } catch (IllegalArgumentException e) {
                /* This should never happen */
                publishProgress("IllegalArgumentException during bind()");
                System.out.println("Received IllegalArgumentException from getSocketAddress()");
                e.printStackTrace();
                return null;
            } catch (BindException e) {
                System.out.println("Received BindException from getSocketAddress()" +
                        " probably already ran. Keep going.");
            } catch (IOException e) {
                publishProgress("IOException during bind()");
                System.out.println("Received IOException from getSocketAddress()");
                //ActivityCompat.requestPermissions(this, new String[]{Manifest.class.});
                e.printStackTrace();
                return null;
            }
            return null;
        }

        private Socket createSocksConnection(String host, int port,
                                             String onionAddr, int onionPort) {
            //Proxy socksOr = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port));
            //Socket sock = new Socket(socksOr);
            Socket sock = new Socket();
            try {
                System.out.println("Initiating connection to Tor");
                sock.connect(new InetSocketAddress(host, port));
                System.out.println("Socket connected, starting SOCKS");
                publishProgress("We connected to Tor's SOCKS port!");
                if (!socks5Init(sock)) {
                    System.out.println("socks5Init returned false");
                    publishProgress("Initial setup with Tor failed");
                    return null;
                }
                System.out.println("Requesting SOCKS CONNECT");
                if (socks5Connect(sock, onionAddr, onionPort) != null) {
                    System.out.println("socks5Connect returned false");
                    return null;
                }
                //sock.connect(new InetSocketAddress(onionAddr, onionPort));
            } catch (IllegalBlockingModeException e) {
                System.out.println("Received IllegalBlockingModeException from createSocksConnection()");
                publishProgress("IllegalBlockingModeException during connect()");
                e.printStackTrace();
                return null;
            } catch (IllegalArgumentException e) {
                System.out.println("Received IllegalArgumentException from createSocksConnection()");
                publishProgress("IllegalArgumentException during connect()");
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                System.out.println("Received IOException from createSocksConnection(), " +
                        host + ":" + port);
                publishProgress("IOException during connect(): " + e.getMessage() +
                        ", " + host + ":" + port);
                e.printStackTrace();
                return null;
            }
            return sock;
        }

        private String sendPing(Socket onionSock) {
            try {
                OutputStream os = onionSock.getOutputStream();
                os.write(new byte[]{'p','i','n','g'});
                os.flush();
                return null;
            } catch (IOException e) {
                return "IOException during waitForPing()";
            }
        }

        private String waitForPing(ServerSocket ss) {
            byte[] ping = new byte[4];
            try {
                ss.setSoTimeout(20);
                Socket client = ss.accept();
                System.out.println("Got connection on onion!");
                InputStream is = client.getInputStream();
                OutputStream os = client.getOutputStream();

                Vector<Byte> response = new Vector<>();
                while (response.size() < 4 && is.read(ping) != -1) {
                    for (Byte b : ping)
                        response.add(b);
                    System.out.println("ping:");
                    for (byte b : ping) {
                        System.out.println(b);
                    }
                    System.out.println("Done.");
                }
                System.out.println("Sending pong");
                os.write(new byte[]{'p','o','n','g'});
                return null;
            } catch (SocketException e) {
                return "SocketException during waitForPing()";
            } catch (IOException e) {
                return "IOException during waitForPing()";
            }
        }

        private String waitForPong(Socket onionSock) {
            byte[] pong = new byte[4];
            try {
                InputStream is = onionSock.getInputStream();
                Vector<Byte> response = new Vector<>();
                while (response.size() < 4 && is.read(pong) != -1) {
                    for (Byte b : pong)
                        response.add(b);
                    System.out.println("pong:");
                    for (byte b : pong) {
                        System.out.println(b);
                    }
                    System.out.println("Done.");
                }
                System.out.println("Got pong");
                return null;
            } catch (IOException e) {
                return "UIException during waitForPong()";
            }
        }

        private boolean socks5Init(Socket sock) {
            byte[] clientSend = new byte[]{0x05, 0x1, 0x0};
            byte[] serverRecv = new byte[2];
            try {
                OutputStream os = sock.getOutputStream();
                os.write(clientSend);
                System.out.println("Sent methods, waiting for response");
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            try {
                Vector<Byte> response = new Vector<>();
                InputStream is = sock.getInputStream();
                while (response.size() < 2 && is.read(serverRecv) != -1) {
                    for (Byte b : serverRecv)
                        response.add(b);
                    System.out.println("response:");
                    for (byte b : serverRecv) {
                        System.out.println(b);
                    }
                    System.out.println("Done.");
                }
                System.out.println("Received SOCKS method response");
                if (response.elementAt(0) != 0x5 || response.elementAt(1) != 0x0)
                    return false;
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        private String socks5Connect(Socket sock, String hostname, int port) {
            byte[] clientSend = new byte[]{0x5,0x01,0x0,0x03};

            Vector<Byte> variableReq = new Vector<>();
            variableReq.add((byte)(hostname.length() & 0xFF));
            for (char c : hostname.toCharArray())
                variableReq.add(((byte) c));
            variableReq.add((byte)((port >> 8) & 0xFF));
            variableReq.add((byte)(port & 0xFF));
            byte[] serverRecv = new byte[20];
            try {
                OutputStream os = sock.getOutputStream();
                System.out.println("Sending SOCKS CONNECT request");
                os.write(clientSend);
                /* TODO This makes me really, really sad; but it isn't a hotspot */
                System.out.println("Sending variable length request: " + variableReq);
                for (Byte b : variableReq) {
                    os.write(b);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "Failure while writing to Tor";
            }

            try {
                Vector<Byte> response = new Vector<>();
                InputStream is = sock.getInputStream();

                System.out.println("Receiving SOCKS CONNECT response");
                while (response.size() < 2 && is.read(serverRecv) != -1) {
                    for (Byte b : serverRecv)
                        response.add(b);
                    System.out.println("response:");
                    for (byte b : serverRecv) {
                        System.out.println(b);
                    }
                    System.out.println("Done.");
                }
                System.out.println("Received SOCKS response");
                if (response.elementAt(0) != 0x5)
                    return "Malformed SOCKS response from Tor";
                switch (response.elementAt(1)) {
                    case 0x0:
                        return null;
                    case 0x01:
                        return "SOCKS CONNECT: General Failure";
                    case 0x02:
                        return "SOCKS CONNECT: Connection not allowed by ruleset";
                    case 0x03:
                        return "SOCKS CONNECT: Network unreachable";
                    case 0x04:
                        return "SOCKS CONNECT: Host unreachable";
                    case 0x05:
                        return "SOCKS CONNECT: Connection refused";
                    case 0x06:
                        return "SOCKS CONNECT: TTL expired";
                    case 0x07:
                        return "SOCKS CONNECT: Command not supported";
                    case 0x08:
                        return "SOCKS CONNECT: Address type not supported";
                }
                return "Unknown error during SOCKS CONNECTS";
            } catch (IOException e) {
                e.printStackTrace();
                return "IOException while reading response";
            }
        }
    }
}
