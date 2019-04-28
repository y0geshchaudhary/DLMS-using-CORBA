package server.model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import centralRepo.interfaces.Repository;
import centralRepo.interfaces.ServerDetail;
import server.database.Database;
import server.interfaces.Book;
import server.interfaces.BookHelper;
import server.interfaces.GeneralException;
import server.interfaces.LibraryOperationsPOA;
import server.interfaces.OperationsEnum;

public class LibraryOperationsImpl extends LibraryOperationsPOA {
	private static final Logger log = LogManager.getLogger(LibraryOperationsImpl.class);

	private Database database;
	private Repository centralRepository;
	private String serverId;
	private ORB orb;
	String[] otherServerIds = { "CON", "MON" };
	
	public LibraryOperationsImpl(String serverId) {
		database = Database.getDatabase();
		database.setServerId(serverId);
		this.serverId = serverId;
	}

	public void setOrb(ORB orb) {
		this.orb = orb;	
	}
	
	public void setCentralRepository(Repository centralRepository) {
		this.centralRepository = centralRepository;
	}

	@Override
	public boolean userExists(String userId){
		log.debug("Inside userExists(String userId) method.");
		log.debug("call parameters: userId-"+userId);
		boolean result = database.userExists(userId);
		log.debug("method call result: "+result);
		return result;
	}

	/**
	 * Manager roles
	 */
	@Override
	public boolean addItem(String managerID, String itemID, String itemName, int quantity) throws GeneralException {
		log.debug("Inside addItem(String managerID, String itemID, String itemName, int quantity) method.");
		log.debug("call parameters: managerID-"+managerID+" ,itemID-"+itemID+" ,itemName-"+itemName+" ,quantity-"+quantity);
		boolean result;
		if (operationIsAllowed(managerID.toUpperCase(), true)) {
			result = database.addBookToLibrary(itemID, new BookImpl(itemID, itemName, quantity));
			log.debug("method call result: "+result);
			return result;
		} else {
			GeneralException e = new GeneralException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing GeneralException.",e);
			throw e;
		}
	}

	@Override
	public int removeItem(String managerID, String itemID, int quantity) throws GeneralException {
		log.debug("Inside removeItem(String managerID, String itemID, int quantity) method.");
		log.debug("call parameters: managerID-"+managerID+" ,itemID-"+itemID+" ,quantity-"+quantity);
		int result;
		if (operationIsAllowed(managerID.toUpperCase(), true)) {
			result = database.removeBooksFromLibrary(itemID, quantity);
			log.debug("method call result: "+result);
			return result;
		} else {
			GeneralException e = new GeneralException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing GeneralException.",e);
			throw e;
		}
	}

	@Override
	public Book[] listAvailableItems(String managerID) throws GeneralException {
		log.debug("Inside listAvailableItems(String managerID) method.");
		log.debug("call parameters: managerID-"+managerID);
		List<BookImpl> bookList;
		if (operationIsAllowed(managerID.toUpperCase(), true)) {
			bookList = database.getAllBooks();
			log.debug("method returning "+bookList.size()+" books.");
			Book[] books = new Book[bookList.size()];
			for (int i = 0; i < bookList.size(); i++)
				try {
					books[i] = BookHelper.narrow(_poa().servant_to_reference(bookList.get(i)));
				} catch (ServantNotActive | WrongPolicy e) {
					GeneralException ge = new GeneralException("Server faced ServantNotActive or WrongPolicy exception.");
					log.error("Server faced ServantNotActive or WrongPolicy exception. Throwing GeneralException.",e);
					throw ge;
				}
			return books;
		} else {
			GeneralException e = new GeneralException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing GeneralException.",e);
			throw e;
		}
	}

	/**
	 * User roles
	 */
	@Override
	public int borrowItem(String userID, String itemID, String numberOfDays) throws GeneralException {
		log.debug("Inside borrowItem(String userID, String itemID, String numberOfDays) method.");
		log.debug("call parameters: userID-"+userID+" ,itemID-"+itemID+" ,numberOfDays-"+numberOfDays);
		userID = userID.toUpperCase();
		itemID = itemID.toUpperCase();
		if (operationIsAllowed(userID, false)) {
			if (itemID.startsWith(serverId)) {
				int result = database.borrowBook(userID, itemID, 1);
				log.debug("request belong to this library. method call returns: "+result);
				return result;
			} else {
				log.debug("request can't be served by this library. Making call to related library over UDP.");
				String data = String.valueOf(OperationsEnum.BORROW_ITEM.value()).concat("#").concat(userID).concat("#").concat(itemID);
				log.debug("Data to send over UDP socket: "+data);
				byte[] dataBytes = data.getBytes();
				String server = itemID.substring(0, 3);
				ServerDetail udpServerDetails = centralRepository.getServerDetails(server + "UDP");
				try (DatagramSocket socket = new DatagramSocket();) {
					DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length,
							InetAddress.getByName(udpServerDetails.getHostname()), udpServerDetails.getPortNumber());
					socket.send(packet);
					dataBytes = new byte[5000];
					packet = new DatagramPacket(dataBytes, dataBytes.length);
					socket.receive(packet);
					data = new String(packet.getData()).trim();
					// if(data.length()!=0) {
					log.debug("response from remote library: "+data);
					return Integer.parseInt(data);
					// }
				} catch (SocketException e) {
					log.error("Unable to open socket connection.");
					e.printStackTrace();
					throw new GeneralException("Unable to open socket connection.");
				} catch (UnknownHostException e) {
					log.error("Unable to identify host given by udpServerDetails");
					e.printStackTrace();
					throw new GeneralException("Unable to identify host given by udpServerDetails.");
				} catch (IOException e) {
					log.error("Issue with sending or receiving data packet.");
					e.printStackTrace();
					throw new GeneralException("Issue with sending or receiving data packet.");
				}
			}

		} else {
			GeneralException e = new GeneralException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing GeneralException.",e);
			throw e;
		}
	}

	@Override
	public Book[] findItem(String userID, String itemName) throws GeneralException {
		log.debug("Inside findItem(String userID, String itemName) method.");
		log.debug("call parameters: userID-"+userID+" ,itemName-"+itemName);
		userID = userID.toUpperCase();
		itemName = itemName.toUpperCase();
		List<BookImpl> bookList = new ArrayList<>();
		if (operationIsAllowed(userID.toUpperCase(), false)) {
			// query local DB
			List<BookImpl> localDBBooksList =database.findItem(itemName);
			log.debug("no. of related books in local library are "+localDBBooksList.size());
			bookList.addAll(localDBBooksList);

			// query other server DB
			String data = String.valueOf(OperationsEnum.FIND_ITEM.value()).concat("#").concat(itemName);
			byte[] dataBytes = data.getBytes();
			log.debug("request data to be send to other libraries is "+data);
			try (DatagramSocket socket = new DatagramSocket();) {
				// get details from Concordia university/
				ServerDetail udpServerDetails = centralRepository.getServerDetails(otherServerIds[0] + "UDP");
				DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length,
						InetAddress.getByName(udpServerDetails.getHostname()), udpServerDetails.getPortNumber());
				socket.send(packet);
				dataBytes = new byte[5000];
				packet = new DatagramPacket(dataBytes, dataBytes.length);
				socket.receive(packet);
				data = new String(packet.getData()).trim();
				String[] bookDetails = data.split("#");
				log.debug("no. of related books received from "+otherServerIds[0]+" are "+bookDetails.length/2);
				
				for (int i = 0; i < bookDetails.length; i+=2) {
					bookList.add(new BookImpl(bookDetails[i], itemName, Integer.parseInt(bookDetails[i+1])));	
				}
				
				data = String.valueOf(OperationsEnum.FIND_ITEM.value()).concat("#").concat(itemName);
				dataBytes = data.getBytes();
				// get details from Montreal university
				udpServerDetails = centralRepository.getServerDetails(otherServerIds[1] + "UDP");
				packet = new DatagramPacket(dataBytes, dataBytes.length,
						InetAddress.getByName(udpServerDetails.getHostname()), udpServerDetails.getPortNumber());
				socket.send(packet);
				dataBytes = new byte[5000];
				packet = new DatagramPacket(dataBytes, dataBytes.length);
				socket.receive(packet);
				data = new String(packet.getData()).trim();
				bookDetails = data.split("#");
				log.debug("no. of related books received from "+otherServerIds[1]+" are "+bookDetails.length/2);
				
				for (int i = 0; i < bookDetails.length; i+=2) {
					bookList.add(new BookImpl(bookDetails[i], itemName, Integer.parseInt(bookDetails[i+1])));	
				}

				log.debug("Total books to be returned are "+bookList.size());
				Book[] books = new Book[bookList.size()];
				for (int i = 0; i < bookList.size(); i++)
					try {
						books[i] = BookHelper.narrow(_poa().servant_to_reference(bookList.get(i)));
					} catch (ServantNotActive | WrongPolicy e) {
						GeneralException ge = new GeneralException("Server faced ServantNotActive or WrongPolicy exception.");
						log.error("Server faced ServantNotActive or WrongPolicy exception. Throwing GeneralException.",e);
						throw ge;
					}
				return books;
			} catch (SocketException e) {
				log.error("Unable to open socket connection.",e);
				e.printStackTrace();
				throw new GeneralException("Unable to open socket connection.");
			} catch (UnknownHostException e) {
				log.error("Unable to identify host given by udpServerDetails",e);
				e.printStackTrace();
				throw new GeneralException("Unable to identify host given by udpServerDetails.");
			} catch (IOException e) {
				log.error("Issue with sending or receiving data packet.",e);
				e.printStackTrace();
				throw new GeneralException("Issue with sending or receiving data packet.");
			}
		} else {
			GeneralException e = new GeneralException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing GeneralException.",e);
			throw e;
		}
	}

	@Override
	public boolean returnItem(String userID, String itemID) throws GeneralException {
		log.debug("Inside returnItem(String userID, String itemID) method.");
		log.debug("call parameters: userID-"+userID+" ,itemID-"+itemID);
		userID = userID.toUpperCase();
		itemID = itemID.toUpperCase();
		if (operationIsAllowed(userID.toUpperCase(), false)) {
			boolean result;
			if (itemID.startsWith(serverId)) {
				result = database.returnBook(userID, itemID);
				log.debug("request belong to this library. method call returns: "+result);
			} else {
				log.debug("request can't be served by this library. Making call to related library over UDP.");
				String data = String.valueOf(OperationsEnum.RETURN_ITEM.value()).concat("#").concat(userID).concat("#").concat(itemID);
				log.debug("Data to send over UDP socket: "+data);
				byte[] dataBytes = data.getBytes();
				String server = itemID.substring(0, 3);
				ServerDetail udpServerDetails = centralRepository.getServerDetails(server + "UDP");
				try (DatagramSocket socket = new DatagramSocket();) {
					DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length,
							InetAddress.getByName(udpServerDetails.getHostname()), udpServerDetails.getPortNumber());
					socket.send(packet);
					dataBytes = new byte[5000];
					packet = new DatagramPacket(dataBytes, dataBytes.length);
					socket.receive(packet);
					data = new String(packet.getData()).trim();
					result = data.equals("TRUE") ? true : false;
					log.debug("response from remote library: "+data);
				} catch (SocketException e) {
					log.error("Unable to open socket connection.",e);
					e.printStackTrace();
					throw new GeneralException("Unable to open socket connection.");
				} catch (UnknownHostException e) {
					log.error("Unable to identify host given by udpServerDetails",e);
					e.printStackTrace();
					throw new GeneralException("Unable to identify host given by udpServerDetails.");
				} catch (IOException e) {
					log.error("Issue with sending or receiving data packet.",e);
					e.printStackTrace();
					throw new GeneralException("Issue with sending or receiving data packet.");
				}
			}
			return result;
		
		} else {
			GeneralException e = new GeneralException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing GeneralException.",e);
			throw e;
		}
	}

	@Override
	public boolean addToWaitingList(String userID, String itemID) throws GeneralException {
		log.debug("Inside addToWaitingList(String userID, String itemID) method.");
		log.debug("call parameters: userID-"+userID+" ,itemID-"+itemID);
		userID = userID.trim();
		itemID = itemID.trim();
		if (operationIsAllowed(userID, false)) {
			boolean result;
			if (itemID.startsWith(serverId)) {
				result = database.addUserToWaitingList(userID, itemID);
				log.debug("request belong to this library. method call returns: "+result);
			} else {
				log.debug("request can't be served by this library. Making call to related library over UDP.");
				String data = String.valueOf(OperationsEnum.ADD_TO_WAITING_LIST.value()).concat("#").concat(userID).concat("#")
						.concat(itemID);
				log.debug("Data to send over UDP socket: "+data);
				byte[] dataBytes = data.getBytes();
				String server = itemID.substring(0, 3);
				ServerDetail udpServerDetails = centralRepository.getServerDetails(server + "UDP");
				try (DatagramSocket socket = new DatagramSocket();) {
					DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length,
							InetAddress.getByName(udpServerDetails.getHostname()), udpServerDetails.getPortNumber());
					socket.send(packet);
					dataBytes = new byte[5000];
					packet = new DatagramPacket(dataBytes, dataBytes.length);
					socket.receive(packet);
					data = new String(packet.getData()).trim();
					result = data.equals("TRUE") ? true : false;
					log.debug("response from remote library: "+data);
				} catch (SocketException e) {
					log.error("Unable to open socket connection.",e);
					e.printStackTrace();
					throw new GeneralException("Unable to open socket connection.");
				} catch (UnknownHostException e) {
					log.error("Unable to identify host given by udpServerDetails",e);
					e.printStackTrace();
					throw new GeneralException("Unable to identify host given by udpServerDetails.");
				} catch (IOException e) {
					log.error("Issue with sending or receiving data packet.",e);
					e.printStackTrace();
					throw new GeneralException("Issue with sending or receiving data packet.");
				}
			}
			return result;
		} else {
			GeneralException e = new GeneralException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing GeneralException.",e);
			throw e;
		}
	}

	private boolean operationIsAllowed(String userId, boolean managerOperation) {
		log.debug("Inside operationIsAllowed(String userId, boolean managerOperation) method.");
		log.debug("call parameters: userId-"+userId+" ,managerOperation-"+managerOperation);
		boolean result;
		if (managerOperation)
			result = userId.charAt(3) == 'M' ? true : false;
		else
			result = userId.charAt(3) == 'U' ? true : false;
		
		log.debug("method call returns: "+result);
		return result;
	}
	
	@Override
	public boolean addToWaitingListOverloaded(String userID, String itemID, String oldItemID) throws GeneralException {
		log.debug("Inside addToWaitingListOverloaded(String userID, String newItemID, String oldItemID) method.");
		log.debug("call parameters: userID-"+userID+" ,newItemID-"+itemID+" ,oldItemID-"+oldItemID);
		userID = userID.trim();
		itemID = itemID.trim();
		oldItemID = oldItemID.trim();
		boolean addToWaitingListResult = this.addToWaitingList(userID, itemID);
		if(addToWaitingListResult) {
			log.debug("added user to waiting list of newItemID.");
			boolean returnResult = this.returnItem(userID, oldItemID);
			if(returnResult) {
				log.debug("returned oldItem to its library.");
				return true;
			}else return false;
			
		}else {
			log.debug("Unable to add to waiting list so returning false.");
			return false;
		}
	}

	/**
	 * return 0 if book wasn't available or user doesn't belong to new book library and want to exchange 
	 * book from same library or wasn't able to exchange book due to any reason, 1 if exchange was successful and -1 if user didn't borrowed the old item.
	 */
	@Override
	public int exchangeItem(String userID, String newItemID, String oldItemID) throws GeneralException {
		log.debug("Inside exchangeItem(String userID, String newItemID, String oldItemID.");
		log.debug("call parameters: userID-"+userID+" ,newItemID-"+newItemID+" ,oldItemID-"+oldItemID);
		userID = userID.trim();
		newItemID = newItemID.trim();
		oldItemID = oldItemID.trim();
		if (operationIsAllowed(userID, false)) {
			boolean bookBorrowed = false;
			boolean bookAvailable = false;
			if (oldItemID.startsWith(serverId)) {
				bookBorrowed = database.bookBorrowed(userID, oldItemID);
			} else {
				log.debug("oldItem belongs to "+oldItemID.substring(0, 3));
				String data = String.valueOf(OperationsEnum.BOOK_BORROWED.value()).concat("#").concat(userID).concat("#")
						.concat(oldItemID);
				log.debug("Data to send over UDP socket: "+data);
				byte[] dataBytes = data.getBytes();
				String server = oldItemID.substring(0, 3);
				ServerDetail udpServerDetails = centralRepository.getServerDetails(server + "UDP");
				try (DatagramSocket socket = new DatagramSocket();) {
					DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length,
							InetAddress.getByName(udpServerDetails.getHostname()), udpServerDetails.getPortNumber());
					socket.send(packet);
					dataBytes = new byte[5000];
					packet = new DatagramPacket(dataBytes, dataBytes.length);
					socket.receive(packet);
					data = new String(packet.getData()).trim();
					bookBorrowed = data.equals("TRUE") ? true : false;
					log.debug("response from remote library: "+data);
				} catch (SocketException e) {
					log.error("Unable to open socket connection.",e);
					e.printStackTrace();
					throw new GeneralException("Unable to open socket connection.");
				} catch (UnknownHostException e) {
					log.error("Unable to identify host given by udpServerDetails",e);
					e.printStackTrace();
					throw new GeneralException("Unable to identify host given by udpServerDetails.");
				} catch (IOException e) {
					log.error("Issue with sending or receiving data packet.",e);
					e.printStackTrace();
					throw new GeneralException("Issue with sending or receiving data packet.");
				}
			}
			log.debug("oldItem was borrowed by this user?: "+bookBorrowed);
			
			if(!bookBorrowed) {
				log.debug("Book "+oldItemID+" is not borrowed by user. So returning response as -1");
				return -1;
			}
			
			if (newItemID.startsWith(serverId)) {
				bookAvailable = database.bookAvailable(newItemID);
			} else {
				log.debug("newItem belongs to "+newItemID.substring(0, 3));
				String data = String.valueOf(OperationsEnum.BOOK_AVAILABLE.value()).concat("#").concat(newItemID);
				log.debug("Data to send over UDP socket: "+data);
				byte[] dataBytes = data.getBytes();
				String server = newItemID.substring(0, 3);
				ServerDetail udpServerDetails = centralRepository.getServerDetails(server + "UDP");
				try (DatagramSocket socket = new DatagramSocket();) {
					DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length,
							InetAddress.getByName(udpServerDetails.getHostname()), udpServerDetails.getPortNumber());
					socket.send(packet);
					dataBytes = new byte[5000];
					packet = new DatagramPacket(dataBytes, dataBytes.length);
					socket.receive(packet);
					data = new String(packet.getData()).trim();
					bookAvailable = data.equals("TRUE") ? true : false;
					log.debug("response from remote library: "+data);
				} catch (SocketException e) {
					log.error("Unable to open socket connection.",e);
					e.printStackTrace();
					throw new GeneralException("Unable to open socket connection.");
				} catch (UnknownHostException e) {
					log.error("Unable to identify host given by udpServerDetails",e);
					e.printStackTrace();
					throw new GeneralException("Unable to identify host given by udpServerDetails.");
				} catch (IOException e) {
					log.error("Issue with sending or receiving data packet.",e);
					e.printStackTrace();
					throw new GeneralException("Issue with sending or receiving data packet.");
				}
			}
			log.debug("newItem is avialable?: "+bookAvailable);
			
			if(!bookAvailable) {
				log.debug("Book is not available in library. User should be put in waiting list.");
				return 0;
			}else {
				int borrowBookResult = -2;
				boolean returnBookResult = false;
				if(bookAvailable && bookBorrowed) {
					log.debug("user borrowed the oldItem and newItem is also available.");
					borrowBookResult = this.borrowItem(userID, newItemID, "0");
					log.debug("result of invoking borrowBook() on newItem server is :"+borrowBookResult);
					// if book is borrowed successfully.
					if(borrowBookResult==1) {
						returnBookResult = this.returnItem(userID, oldItemID);
						log.debug("borrowed newItem and result of returning oldItem :"+returnBookResult);
						// if book was returned successfully then return 1.
						if(returnBookResult) {
							log.debug("Items were exchanged successfully.");
							return 1;
						}
						//if not then return the newly borrowed book.
						else {
							log.debug("Unable to return oldItem so returning newItem.");
							this.returnItem(userID, newItemID);
							return 0;
						}
					}
					// if book is not borrowed and borrowBookResult = 1 or use doesn't belong to 
					// newItem library but still want to exchange book against the book which belong 
					// to same library or he already have a different book from that library and and borrowBookResult = 2
					// so return 0 so that user can add himself to waiting list and return oldItem.
					else return 0;
				}
				// not able to exchange items so by default just enter in waiting list by returning the book.
				else
					return 0;
			}
			
		} else {
			GeneralException e = new GeneralException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing AccessException.",e);
			throw e;
		}
	}
	
}
