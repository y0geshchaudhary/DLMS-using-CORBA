package centralrepository.model;

import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import centralRepo.interfaces.RepositoryPOA;
import centralRepo.interfaces.ServerDetail;
import centralRepo.interfaces.ServerDetailHelper;

public class RepositoryImpl extends RepositoryPOA {

	private static final Logger log = LogManager.getLogger(RepositoryImpl.class);
	private HashMap<String, ServerDetailImpl> libServers;
	private ORB orb;
	
	public RepositoryImpl(){
		this.libServers = new LinkedHashMap<String, ServerDetailImpl>();
	}

	public void setOrb(ORB orb) {
		this.orb = orb;
	}

	@Override
	public boolean registerLibraryServer(String library, String hostname, int port, String stubName) {
		log.debug("Inside registerLibraryServer() method.");
		log.debug("method param are: library="+library+", hostname="+hostname+", port="+port+", stubName="+stubName);
		ServerDetailImpl server = new ServerDetailImpl(hostname, port, stubName);
		libServers.put(library, server);
		return true;
	}

	@Override
	public ServerDetail getServerDetails(String libId){
		log.debug("Inside getServerDetails() method.");
		log.debug("method param are: libId="+libId);
		ServerDetailImpl serverDetails = libServers.get(libId);
		org.omg.CORBA.Object obj = null;
		try {
			obj = _poa().servant_to_reference(serverDetails);
		} catch (ServantNotActive | WrongPolicy e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
		return ServerDetailHelper.narrow(obj);
	}

}
