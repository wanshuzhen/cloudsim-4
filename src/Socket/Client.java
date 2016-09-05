package Socket;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {

	public static void main(String[] args) throws UnknownHostException, IOException {
		
		Socket socket = new Socket("2001:da8:9000:a811:c930:9882:dcf4:8e41",10000);
		OutputStream out = socket.getOutputStream();
		
		
	}

}
