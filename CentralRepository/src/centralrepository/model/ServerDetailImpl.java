package centralrepository.model;

import centralRepo.interfaces.ServerDetailPOA;

public class ServerDetailImpl extends ServerDetailPOA {
	private String hostname;
	private int port;
	private String stubName;

	protected ServerDetailImpl(String hostname, int port, String stubName){
		this.hostname = hostname;
		this.port = port;
		this.stubName = stubName;
	}

	@Override
	public String getHostname() {
		return this.hostname;
	}

	@Override
	public int getPortNumber() {
		return this.port;
	}

	@Override
	public String getStubName() {
		return this.stubName;
	}

}
