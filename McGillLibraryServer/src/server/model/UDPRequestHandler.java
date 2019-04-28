package server.model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import server.database.Database;
import server.interfaces.OperationsEnum;

public class UDPRequestHandler implements Runnable {
	private static final Logger log = LogManager.getLogger(UDPRequestHandler.class);
	private DatagramPacket packet;

	public UDPRequestHandler(DatagramPacket packet) {
		super();
		this.packet = packet;
	}

	@Override
	public void run() {
		log.debug("Inside run() method.");
		byte[] data = packet.getData();
		if (data == null || data.length == 0)
			return;
		else {
			String dataString = new String(data);
			String[] dataArray = dataString.trim().split("#");
			int operation = Integer.parseInt(dataArray[0]);
			Database database = Database.getDatabase();
			log.debug("Data string received for processing is - "+dataString);
			
			DatagramPacket replyPacket;
			boolean resultBool;
			String resultString = null;
			int resultInt;
			try (DatagramSocket socket = new DatagramSocket();) {
				switch (operation) {
				
					// request = dataString format OperationsEnumNo#userId#itemId
					// response = dataString format int
				case OperationsEnum._BORROW_ITEM:
					log.debug("Operation reuqested: BORROW_ITEM");
					resultInt = database.borrowBook(dataArray[1], dataArray[2], 0);
					resultString = String.valueOf(resultInt);
					replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
							packet.getAddress(), packet.getPort());
					socket.send(replyPacket);
					log.debug("Result of operation : "+resultString);
					break;
					
					// request = dataString format OperationsEnumNo#userId#itemId
					// response = dataString format string as TRUE/FALSE
				case OperationsEnum._ADD_TO_WAITING_LIST:
					log.debug("Operation reuqested: ADD_TO_WAITING_LIST");
					resultBool = database.addUserToWaitingList(dataArray[1], dataArray[2]);
					resultString = resultBool ? "TRUE" : "FALSE";
					replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
							packet.getAddress(), packet.getPort());
					socket.send(replyPacket);
					log.debug("Result of operation : "+resultString);
					break;
				
					// request = dataString format OperationsEnumNo#itemName
					// response = dataString format bookId#numberOfCopies#bookId#numberOfCopies
				case OperationsEnum._FIND_ITEM:
					log.debug("Operation reuqested: FIND_ITEM");
					List<BookImpl> books = database.findItem(dataArray[1]);
					if(books!=null || books.size()>0) {
						for (Iterator iterator = books.iterator(); iterator.hasNext();) {
							BookImpl bookImpl = (BookImpl) iterator.next();
							resultString = bookImpl.getId().concat("#").concat(String.valueOf(bookImpl.getNumberOfCopies())).concat("#");
						}
						resultString = resultString.substring(0, resultString.length()-1);
						
					} else resultString = "";
					replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
							packet.getAddress(), packet.getPort());
					socket.send(replyPacket);
					log.debug("Result of operation : "+resultString);
					break;
				
					// request = dataString format OperationsEnumNo#userId#itemId
					// response = dataString format string as TRUE/FALSE
				case OperationsEnum._RETURN_ITEM:
					log.debug("Operation reuqested: RETURN_ITEM");
					resultBool = database.returnBook(dataArray[1], dataArray[2]);
					resultString = resultBool ? "TRUE" : "FALSE";
					replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
							packet.getAddress(), packet.getPort());
					socket.send(replyPacket);
					log.debug("Result of operation : "+resultString);
					break;
				
					// request = dataString format OperationsEnumValue#userId#oldItemId
					// response = dataString format string as TRUE/FALSE
				case OperationsEnum._BOOK_BORROWED:
					log.debug("Operation reuqested: BOOK_BORROWED");
					resultBool = database.bookBorrowed(dataArray[1], dataArray[2]);
					resultString = resultBool ? "TRUE" : "FALSE";
					replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
							packet.getAddress(), packet.getPort());
					socket.send(replyPacket);
					log.debug("Result of operation : "+resultString);
					break; 
					
					// request = dataString format OperationsEnumValue#newItemId
					// response = dataString format string as TRUE/FALSE
				case OperationsEnum._BOOK_AVAILABLE:
					log.debug("Operation reuqested: BOOK_AVAILABLE");
					resultBool = database.bookAvailable(dataArray[1]);
					resultString = resultBool ? "TRUE" : "FALSE";
					replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
							packet.getAddress(), packet.getPort());
					socket.send(replyPacket);
					log.debug("Result of operation : "+resultString);
					break;
				
				default:
					log.debug("Default Operation.");
					replyPacket = new DatagramPacket(new byte[0], 0, packet.getAddress(), packet.getPort());
					socket.send(replyPacket);
					log.debug("Returning empty byte array.");
				}
			} catch (SocketException e) {
				log.error("Issue with opening socket connection.",e);
				e.printStackTrace();
			} catch (IOException e) {
				log.error("Issue with sending data packet.",e);
				e.printStackTrace();
			}
		}

	}

}
