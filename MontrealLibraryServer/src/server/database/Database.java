package server.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import server.model.BookImpl;

public class Database {
	private static final Logger log = LogManager.getLogger(Database.class);
	// map with key as bookId and value as Book object
	private static final Map<String, BookImpl> bookDB = Collections.synchronizedMap(new LinkedHashMap<>());
	// Set of String as userId
	private static final Set<String> users = Collections.synchronizedSet(new LinkedHashSet<>());
	// map with key as bookId and value as list of userId
	private static final Map<String, List<String>> waitingList = Collections.synchronizedMap(new LinkedHashMap<>());
	// map with key as bookId and value as list of userId
	private static final Map<String, List<String>> borrowedBooks = Collections.synchronizedMap(new LinkedHashMap<>());
	private static Database db;
	private String serverId;

	private Database() {
		createDatabaseEntries();
	}

	public static Database getDatabase() {
		if (db == null)
			db = new Database();

		return db;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	private void createDatabaseEntries() {
		log.debug("Inside createDatabaseEntries() method.");
		synchronized (users) {
			Collections.addAll(users, "MONM1111", "MONM1112", "MONM1113", "MONM1114", "MONM1115", "MONU1111",
					"MONU1112", "MONU1113", "MONU1114", "MONU1115", "MONU1116", "MONU1117", "MONU1118", "MONU1119",
					"MONU1120");
		}
		log.debug("No. of users added to DB are " + users.size());
		List<String> bookIds = new LinkedList<>();
		List<String> bookNames = new LinkedList<>();
		int numberOfCopies = 5;
		Collections.addAll(bookIds, "MON6231", "MON6641", "MON6491", "MON6651", "MON6481", "MON6501", "MON6411",
				"MON6180", "MON6461", "MON6521");
		Collections.addAll(bookNames, "Distributed Systems", "Advanced Programming", "Systems Software",
				"Algorithm Design", "System Requirements Spec", "Programming Competency", "Comparative Studies",
				"Data Mining", "Software Design", "Advance Database");
		synchronized (bookDB) {
			BookImpl book;
			for (int i = 0; i < bookIds.size(); i++) {
				book = new BookImpl(bookIds.get(i), bookNames.get(i), numberOfCopies);
				bookDB.put(book.getId(), book);
			}
			log.debug("No. of books added to DB are " + bookDB.size());
		}
	}

	public boolean userExists(String userID) {
		log.debug("Inside userExists(String userID) method.");
		log.debug("call parameters: userID-" + userID);
		synchronized (users) {
			log.debug("return value: " + users.contains(userID));
			return users.contains(userID);
		}
	}

	public boolean addBookToLibrary(String itemID, BookImpl book)/* throws RemoteException */ {
		log.debug("Inside addBookToLibrary(String itemID, BookImpl book) method.");
		log.debug("call parameters: itemID-" + itemID + " , book-" + book);
		BookImpl b;
		synchronized (bookDB) {
			synchronized (waitingList) {
				synchronized (borrowedBooks) {
					if (bookDB.containsKey(itemID)) {
						b = bookDB.get(itemID);
						b.setNumberOfCopies(b.getNumberOfCopies() + book.getNumberOfCopies());
						log.debug("Book is already in DB, incremented it's quantity.");
					} else {
						bookDB.put(itemID, book);
						log.debug("Book is not in DB, adding book to DB.");
					}
					sweepWaitingList();
					return true;
				}
			}
		}
	}

	// return 0 if operation is unsuccessful, 1 if its successful and 2 if input is
	// greater than available books in library and 3 if nothing is available.
	public int removeBooksFromLibrary(String itemID, int quantity)/* throws RemoteException */ {
		log.debug("Inside removeBooksFromLibrary(String itemID, int quantity) method.");
		log.debug("call parameters: itemID-" + itemID + " , quantity-" + quantity);
		BookImpl b;
		synchronized (bookDB) {
			synchronized (borrowedBooks) {
				if (bookDB.containsKey(itemID)) {
					b = bookDB.get(itemID);
					if (quantity == -1) {
						bookDB.remove(b.getId());
						borrowedBooks.remove(b.getId());
						log.debug("Book is completely removed from DB.");
						return 1;
					} else if (quantity <= b.getNumberOfCopies()) {
						b.setNumberOfCopies(b.getNumberOfCopies() - quantity);
						log.debug("No. of copies decremented by quantity.");
						return 1;
					} else if (quantity > b.getNumberOfCopies()) {
						log.debug("Quantity to decrease is higher than available books in library. So doing nothing.");
						return 2;
					} else {
						log.debug("Returning without changing DB.");
						return 0;
					}

				} else {
					log.debug("There is no item found in library to remove it.");
					return 3;
				}
			}
		}
	}

	public List<BookImpl> getAllBooks() {
		log.debug("Inside getAllBooks() method.");
		synchronized (bookDB) {
			List<BookImpl> list = new ArrayList<>(bookDB.values());
			log.debug("Returning " + list.size() + " books.");
			return list;
		}
	}

	public boolean returnBook(String userID, String itemID)/* throws RemoteException */ {
		log.debug("Inside returnBook(String userID, String itemID) method.");
		log.debug("call parameters: userID-" + userID + " , itemID-" + itemID);
		// return the book to library and assign it to user if there is any in waiting
		// list for that book.
		synchronized (borrowedBooks) {
			synchronized (bookDB) {
				synchronized (waitingList) {
					if (borrowedBooks.containsKey(itemID) && borrowedBooks.get(itemID).contains(userID)) {
						if (borrowedBooks.get(itemID).remove(userID)) {
							BookImpl book = bookDB.get(itemID);
							book.setNumberOfCopies(book.getNumberOfCopies() + 1);
							log.debug("Book returned to library.");
							sweepWaitingList();
							return true;
						} else
							return false;
					} else
						return false;
				}
			}
		}
	}

	public List<BookImpl> findItem(String itemName)/* throws RemoteException */ {
		log.debug("Inside findItem(String itemName) method.");
		log.debug("call parameters: itemName-" + itemName);
		// look in map and return it.
		List<BookImpl> books;
		synchronized (bookDB) {
			books = new ArrayList<>();
			BookImpl book;
			for (Iterator<String> iterator = bookDB.keySet().iterator(); iterator.hasNext();) {
				String string = (String) iterator.next();
				book = bookDB.get(string);
				if (book.getName().equalsIgnoreCase(itemName))
					books.add(book);
			}
		}
		log.debug("no of books found with itemName are " + books.size());
		return books;

	}

	// 0 if user is not from this Library else 1
	public int borrowBook(String userID, String itemID, int thisLibraryUser) /* throws RemoteException */ {
		log.debug("Inside borrowBook(String userID, String itemID, int thisLibraryUser) method.");
		log.debug("call parameters: userID-" + userID + " ,itemID-" + itemID + " ,thisLibraryUser-" + thisLibraryUser);
		// return -1 if the book doesn't exist in library, 0 if it isn't borrowed, 1 if
		// book is borrowed and 2 if user can't
		// borrow more items from this library.
		synchronized (bookDB) {
			synchronized (borrowedBooks) {
				if (!bookDB.containsKey(itemID))
					return -1;
				else {
					// updated as per the requirements that -
					// a user can borrow multiple items in their own library, but only 1 item from
					// each of the other libraries.
					if (thisLibraryUser == 0 && userAlreadyHaveBook(userID)) {
						log.debug(
								"This user belongs to different university and already borrowed a book from this library.");
						return 2;
					} else {
						BookImpl book = bookDB.get(itemID);
						if (book.getNumberOfCopies() > 0) {
							book.setNumberOfCopies(book.getNumberOfCopies() - 1);
							if (borrowedBooks.containsKey(itemID)) {
								borrowedBooks.get(itemID).add(userID);
							} else {
								List<String> tempList = new LinkedList<>();
								tempList.add(userID);
								borrowedBooks.put(itemID, tempList);
							}
							log.debug("Assigned requested book to user.");
							return 1;
						} else {
							log.debug("Unable to assign requested book to user.");
							return 0;
						}
					}
				}
			}
		}

	}

	public boolean addUserToWaitingList(String userID, String itemID) {
		log.debug("Inside addUserToWaitingList(String userID, String itemID) method.");
		log.debug("call parameters: userID-" + userID + " ,itemID-" + itemID);
		synchronized (waitingList) {
			if (waitingList.containsKey(itemID) && waitingList.get(itemID) != null) {
				waitingList.get(itemID).add(userID);
			} else {
				List<String> userList = new ArrayList<>();
				userList.add(userID);
				waitingList.put(itemID, userList);
			}
			log.debug("Added userId to waiting list.");
			return true;
		}

	}

	private boolean userAlreadyHaveBook(String userID) {
		boolean haveBook = false;
		for (String user : borrowedBooks.keySet()) {
			if (borrowedBooks.get(user).contains(userID))
				haveBook = true;
		}

		return haveBook;
	}

	public boolean bookBorrowed(String userID, String itemID) {
		log.debug("Inside bookBorrowed(String userID, String itemID) method.");
		log.debug("call parameters: userID-" + userID + " ,itemID-" + itemID);
		synchronized (borrowedBooks) {
			if (borrowedBooks.containsKey(itemID) && borrowedBooks.get(itemID) != null
					&& borrowedBooks.get(itemID).contains(userID)) {
				log.debug("requested user has borrowed this item.");
				return true;
			} else {
				log.debug("requested user has not borrowed this item.");
				return false;
			}
		}
	}

	public boolean bookAvailable(String itemID) {
		log.debug("Inside bookAvailable(String itemID) method.");
		log.debug("call parameters: itemID-" + itemID);
		synchronized (bookDB) {
			if (bookDB.containsKey(itemID) && bookDB.get(itemID) != null
					&& bookDB.get(itemID).getNumberOfCopies() > 0) {
				log.debug("requested item is available.");
				return true;
			} else {
				log.debug("requested item is not available.");
				return false;
			}
		}
	}

	private void sweepWaitingList() {
		log.debug("Running sweep for waiting users.");
		Set<String> otherLibUsers = new HashSet<>();
		for (List<String> borrowList : borrowedBooks.values()) {
			for (String user : borrowList) {
				if (!user.startsWith(serverId))
					otherLibUsers.add(user);
			}
		}
		int userWaiting, bookCopies, iterations = 0;
		List<String> userList = null;
		List<String> assignBooksTo = new ArrayList<>();
		BookImpl book = null;
		String userId = null;
		List<String> borrowedBookList = null;
		for (String bookId : waitingList.keySet()) {
			assignBooksTo.clear();
			userList = new ArrayList<>(waitingList.get(bookId));
			book = bookDB.get(bookId);
			if (userList != null && userList.size() > 0 && book != null && book.getNumberOfCopies() > 0) {
				bookCopies = book.getNumberOfCopies();
				userWaiting = userList.size();
				iterations = userWaiting <= bookCopies ? userWaiting : bookCopies;
				borrowedBookList = borrowedBooks.get(bookId);

				for (int i = 0; i < iterations && userList.size() > i; i++) {
					userId = userList.get(i);
					log.debug("User " + userId + " is waiting for " + book.getId() + ".");
					if (userId.startsWith(serverId) || !otherLibUsers.contains(userId)) {
						if (borrowedBookList != null) {
							borrowedBookList.add(userId);
							// userList.remove(i);
							book.setNumberOfCopies(book.getNumberOfCopies() - 1);
						} else {
							List<String> tempUserList = new LinkedList<>();
							tempUserList.add(userId);
							// userList.remove(i);
							book.setNumberOfCopies(book.getNumberOfCopies() - 1);
							borrowedBooks.put(bookId, tempUserList);
						}
						if (!userId.startsWith(serverId))
							otherLibUsers.add(userId);
						assignBooksTo.add(userId);
						log.debug("Assigned " + book.getId() + " to " + userId + ".");
					} else {
						i--;
						userList.remove(userId);
						log.debug(userId + " already borrowed a book from this library.");
					}
				}
			}
			for (String user : assignBooksTo) {
				waitingList.get(bookId).remove(user);
			}
		}
		log.debug("waiting list sweep complete.");
	}
}
