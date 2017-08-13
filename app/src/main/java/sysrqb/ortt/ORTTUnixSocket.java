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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketImpl;

public class ORTTUnixSocket extends SocketImpl {
    private int fd = 0;
    private ByteArrayInputStream inputStream = null;
    private ByteArrayOutputStream outputStream = null;
    public ORTTUnixSocket() {
        inputStream = new ByteArrayInputStream(new byte[4096]);
        outputStream = new ByteArrayOutputStream();
    }

    protected InputStream getInputStream() {
        return inputStream;
    }

    protected OutputStream getOutputStream() {
        return outputStream;
    }

    protected void close() {
        // TODO
    }

    protected void accept(SocketImpl s) {
    }

    protected int available() {
        return inputStream.available();
    }

    protected void bind(InetAddress host, int port) {}
    protected void connect(InetAddress address, int port) {}
    protected void connect(SocketAddress address, int timeout) {}
    protected void connect(String host, int port) {}

    protected void create(boolean stream) {}
    protected void listen(int backlog) {}
    protected void sendUrgentData(int data) {}

    public Object getOption(int optID) {return null;}
    public void setOption(int optID, Object value) {}

    private native int connectImpl(String path);

    public void connect(String path) throws IOException {
        fd = connectImpl(path);
        System.out.println("connectImpl returned " + fd);
        if (fd < 0)
            throw new IOException("Connect Error");
    }

    private native long readImpl(byte[] buf, int size);

    public long read(byte[] buf, int size) {
        return readImpl(buf, size);
    }

    private native long writeImpl(byte[] buf, int size);

    public long write(byte[] buf, int size) {
        return writeImpl(buf, size);
    }

    public long write(String s) {
        return write(s.getBytes(), s.length());
    }
}
