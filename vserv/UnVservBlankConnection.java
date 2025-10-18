/*
Copyright (c) 2025 Arman Jussupgaliyev
*/
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.InputConnection;

public class UnVservBlankConnection implements InputConnection {

	public DataInputStream openDataInputStream() throws IOException {
		return new DataInputStream(openInputStream());
	}

	public InputStream openInputStream() throws IOException {
		return new ByteArrayInputStream(new byte[0]);
	}

	public void close() {
	}

}
