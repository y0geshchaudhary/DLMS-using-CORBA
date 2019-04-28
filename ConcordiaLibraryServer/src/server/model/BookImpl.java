package server.model;

import server.interfaces.BookPOA;

public class BookImpl extends BookPOA {

	private String name;
	private String id;
	private int numberOfCopies;

	public BookImpl(String id, String name, int numberOfCopies) {
		this.name = name;
		this.id = id;
		this.numberOfCopies = numberOfCopies;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return id;
	}

	@Override
	public int getNumberOfCopies() {
		// TODO Auto-generated method stub
		return numberOfCopies;
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		this.name = name;

	}

	@Override
	public void setId(String id) {
		// TODO Auto-generated method stub
		this.id = id;
	}

	@Override
	public void setNumberOfCopies(int numberOfCopies) {
		// TODO Auto-generated method stub
		this.numberOfCopies = numberOfCopies;
	}

}
