/*  ORTT - The Onion Ring Time Trial
 *  Copyright (C) 2017  Matthew Finkel
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package sysrqb.ortt;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;

public class TorController {
    private String controlPortPath = "";
    private ORTTUnixSocket controlSocket;

    protected String controlPortCookiePath = "";

    protected enum AuthMethod {
        NULL,
        HASHEDPASSWORD,
        COOKIE,
        SAFECOOKIE,
        UNKNOWN
    }
    private int maxLength = 4096;

    public TorController() {
        /* We'll just make some assumptions here, such
           Tor listening on a Unix Domain Socket.
         */
        controlSocket = new ORTTUnixSocket();
    }

    public boolean connectToControlSocket() {
        try {
            controlSocket.connect(controlPortPath);
        } catch (IOException e) {
            return false;
        }
        Hashtable<String, String> protoinfo = confirmProtocolInfo();
        if (controlSocketNeedsAuth(protoinfo)) {
            return authenticate(chooseAuthMethod(protoinfo));
        }
        return true;
    }

    private Hashtable<String,String> confirmProtocolInfo() {
        String protocolInfoCmd = "PROTOCOLINFO\r\n";
        String response = "";
        byte[] resBytes = new byte[maxLength];
        long bytes_read;

        while (true) {
            controlSocket.write(protocolInfoCmd);
            bytes_read = controlSocket.read(resBytes, maxLength);
            if (bytes_read == -1)
                break;
            response += resBytes;
        }

        return parseProtocolInfoResponse(response);
    }

    private boolean controlSocketNeedsAuth(Hashtable<String, String> protoinfo) {
        String auths = protoinfo.get("AUTH");
        if (auths == null) {
            /* TODO This is actually an error... */
            return false;
        }

        String[] authline = auths.split(" ");
        if (authline.length == 0) {
            /* TODO Another error... */
            return false;
        }
        return true;
    }

    protected AuthMethod chooseAuthMethod(Hashtable<String, String> protoinfo) {
        ControllerAuthMethods authMeth = new ControllerAuthMethods(protoinfo);
        controlPortCookiePath = null;
        if (!authMeth.parseAuthMethods()) {
            return AuthMethod.UNKNOWN;
        }
        if (authMeth.isNullAuthSupported()) {
            return AuthMethod.NULL;
        }
        if (authMeth.isSafeCookieAuthSupported()) {
            controlPortCookiePath = authMeth.getAuthCookieFileName();
            return AuthMethod.SAFECOOKIE;
        }
        if (authMeth.isHashedPasswordAuthSupported()) {
            return AuthMethod.HASHEDPASSWORD;
        }
        if (authMeth.isCookieAuthSupported()) {
            return AuthMethod.COOKIE;
        }
        return AuthMethod.UNKNOWN;
    }

    private String openCookieFile(String path) {
        FileInputStream in;
        try {
            in = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            return null;
        }

        byte[] cookie = new byte[32];
        try {
            in.read(cookie, 0, 32);
            return new String(cookie);
        } catch (IOException e) {
            return null;
        }
    }

    protected String constructAuthLine(AuthMethod meth, String cookie) {
        String authenticateCmd = "AUTHENTICATE";
        switch (meth) {
            case NULL:
                break;
            case SAFECOOKIE:
                if (cookie.length() != 32)
                    return null;
                authenticateCmd += " " + cookie;
                break;
            case UNKNOWN:
            case HASHEDPASSWORD:
            case COOKIE:
                return null;
        }
        authenticateCmd += "\r\n";
        return authenticateCmd;
    }

    public boolean authenticate(AuthMethod meth) {
        byte[] buf = new byte[4096];
        String cookie = "";
        if (controlPortCookiePath != null)
            cookie = openCookieFile(controlPortCookiePath);

        String authenticateCmd = constructAuthLine(meth, cookie);
        if (authenticateCmd == null)
            return false;
        controlSocket.write(authenticateCmd);
        long bytesRead = controlSocket.read(buf, buf.length);
        return authenticateSuccessful(bytesRead, buf);
    }

    protected boolean authenticateSuccessful(long bytesRead, byte[] response) {
        String res =  new String(response);
        if (bytesRead == 0)
            return false;
        if (res.startsWith("515 Bad Authentication"))
            return false;
        if (res.startsWith("250 OK"))
            return true;
        return false;
    }

    protected Hashtable<String, String> parseProtocolInfoResponse(String lines) {
        if (lines == null)
            return null;

        String[] linelist = lines.split("\r\n");
        if (linelist.length == 0)
            return null;

        if (!linelist[0].startsWith("250-PROTOCOLINFO")) {
            return null;
        }
        int len250hy = "250-".length();
        Hashtable<String, String> protoinfo = new Hashtable<>(10, 0.75f);
        for (String line : linelist) {
            if (!line.startsWith("250"))
                return null;

            int firstSpace = line.indexOf(' ');
            if (firstSpace == -1) {
                /* Assume this line is purely informational */
                firstSpace = line.length();
            } else if (firstSpace == len250hy - 1) {
                /*
                    The last line contains "250 OK". Cheat by moving
                    firstSpace to the end of the line.
                 */
                firstSpace = line.length();
            }
            String infoKey = line.substring(len250hy, firstSpace);
            String infoVal = "";
            if (firstSpace != line.length()) {
                infoVal = line.substring(firstSpace + 1);
            }
            protoinfo.put(infoKey, infoVal);
        }
        return protoinfo;
    }

    private class ControllerAuthMethods {
        private boolean nullAuthSupported = false;
        private boolean hashPasswordAuthSupported = false;
        private boolean cookieAuthSupported = false;
        private boolean safeCookieAuthSupported = false;
        private String cookiePath = "";
        private Hashtable<String, String> protoinfo;

        private ControllerAuthMethods(Hashtable<String, String> protoinfo) {
            this.protoinfo = protoinfo;
        }

        private boolean parseAuthMethods() {
            if (protoinfo == null)
                return false;

            String auths = protoinfo.get("AUTH");
            if (auths == null)
                return false;

            String[] authline = auths.split(" ");
            if (!authline[0].startsWith("METHOD=")) {
                return false;
            }

            String supportedMethodsList = authline[0].substring("METHOD=".length());
            String[] supportedMethods = supportedMethodsList.split(",");
            if (supportedMethods.length == 0) {
                return false;
            }

            for (String meth : supportedMethods) {
                switch (meth) {
                    case "NULL":
                        nullAuthSupported = true;
                        break;
                    case "HASHEDPASSWORD":
                        hashPasswordAuthSupported = true;
                        break;
                    case "COOKIE":
                        cookieAuthSupported = true;
                        break;
                    case "SAFECOOKIE":
                        safeCookieAuthSupported = true;
                        break;
                    default:
                    /* What's this? */
                    return false;
                }
            }

            if (authline.length > 1 && safeCookieAuthSupported) {
                cookiePath = getCookieFile(authline[1]);
            }
            return true;
        }

        private String getCookieFile(String cookieFile) {
            if (!cookieFile.startsWith("COOKIEFILE=")) {
                return null;
            }
            /* TODO Deleting escaping is annoying. */
            String path = cookieFile.substring("COOKIEFILE=".length());
            /* AuthCookieFile is a QuotedString, delete the
               surrounding quotes */
            if (path.startsWith("\""))
                path = path.substring(1);
            if (path.endsWith("\""))
                path = path.substring(0, path.length()-1);
            return path;
        }

        private boolean isNullAuthSupported() {
            return nullAuthSupported;
        }

        private boolean isHashedPasswordAuthSupported() {
            return hashPasswordAuthSupported;
        }

        private boolean isCookieAuthSupported() {
            return cookieAuthSupported;
        }

        private boolean isSafeCookieAuthSupported() {
            return safeCookieAuthSupported;
        }

        private String getAuthCookieFileName() {
            return cookiePath;
        }
    }
}
