/*
Copyright (c) 2025 Arman Jussupgaliyev
*/
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.InputConnection;
import javax.microedition.io.OutputConnection;

public class UnVservConnector {
	
	public static Connection open(String name) throws IOException {
		return open(name, Connector.READ_WRITE);
	}

	public static Connection open(String name, int mode) throws IOException {
		return open(name, mode, false);
	}

	public static Connection open(String name, int mode, boolean timeouts) throws IOException {
		if (name != null) {
			if ("resource://!blank".equalsIgnoreCase(name)) {
				return new UnVservBlankConnection();
			}
			if (name.startsWith("vserv:")
					|| (name.startsWith("http:") && (name.startsWith("http://a.vserv.mobi/")
					|| (name.indexOf("vserv.mobi/") != -1 && name.indexOf("/adapi") != -1)))) {
				return new UnVservHttpConnection(name);
			}
		}
		return Connector.open(name, mode, timeouts);
	}

	public static DataInputStream openDataInputStream(String name) throws IOException {
		return ((InputConnection) open(name, Connector.READ)).openDataInputStream();
	}

	public static DataOutputStream openDataOutputStream(String name) throws IOException {
		return ((OutputConnection) open(name, Connector.WRITE)).openDataOutputStream();
	}

	public static InputStream openInputStream(String name) throws IOException {
		return openDataInputStream(name);
	}

	public static OutputStream openOutputStream(String name) throws IOException {
		return openDataOutputStream(name);
	}
	
}
