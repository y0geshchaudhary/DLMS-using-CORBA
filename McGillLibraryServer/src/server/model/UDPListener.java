package server.model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UDPListener implements Runnable {
	private static final Logger log = LogManager.getLogger(UDPListener.class);
	private int port;
	
	public UDPListener(int port) {
		super();
		this.port = port;
	}

	@Override
	public void run() {
		log.debug("Inside run() method.");
		try(DatagramSocket  socket = new DatagramSocket(port)) {
			log.debug("UDP sockket is open at "+port+" port to listen for request.");
			while(true) {
				byte[] data = new byte[5000];
				DatagramPacket packet = new DatagramPacket(data, data.length);
				socket.receive(packet);
				Thread reqestHandler = new Thread(new UDPRequestHandler(packet));
				reqestHandler.start();
			}
		} catch (SocketException e) {
			log.error("Issue with opening socket connection over "+port,e);
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Issue with creating packet from data received on socket.",e);
			e.printStackTrace();
		}
	}

}
