package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import centralRepo.interfaces.Repository;
import centralRepo.interfaces.RepositoryHelper;
import server.interfaces.LibraryOperations;
import server.interfaces.LibraryOperationsHelper;
import server.model.LibraryOperationsImpl;
import server.model.UDPListener;

public class ConLibraryServerMain {
	private static final Logger log = LogManager.getLogger(ConLibraryServerMain.class);

	public static void main(String[] args) {
		log.debug("Inside main() method.");
		// SERVER and UDP details.
		String library = "CON";
		String libraryUDP = "CONUDP";
		int portUDP = 2001;
		String stubName = "ConcordiaLibrary";
		LibraryOperationsImpl libraryOperationsImpl = new LibraryOperationsImpl(library);
		NamingContextExt ncRef = null;
		ORB orb = null;

		// binding remote object to ORB
		String[] arg = { "-ORBInitialPort", String.valueOf(Repository.PORT), "-ORBInitialHost localhost" };
		try {
			orb = ORB.init(arg, null);
			POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
			rootpoa.the_POAManager().activate();

			libraryOperationsImpl.setOrb(orb);

			org.omg.CORBA.Object obj = rootpoa.servant_to_reference(libraryOperationsImpl);
			LibraryOperations libraryOperations = LibraryOperationsHelper.narrow(obj);

			org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
			ncRef = NamingContextExtHelper.narrow(objRef);

			NameComponent path[] = ncRef.to_name(stubName);
			ncRef.rebind(path, libraryOperations);

		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}

		// setting up UDP listener thread.
		Thread udpThread = new Thread(new UDPListener(portUDP));
		udpThread.start();
		log.debug("Starting UDP thread.");

		// fetching central repository details.
		Repository repository = null;
		try {
			repository = RepositoryHelper.narrow(ncRef.resolve_str(Repository.STUBNAME));
		} catch (NotFound | CannotProceed | InvalidName e) {
			log.error("Issue with fetching central repository reference.", e);
			e.printStackTrace();
		}

		// registering Concordia server details and UDP socket details to central
		// repository.
		repository.registerLibraryServer(libraryUDP, Repository.HOSTNAME, portUDP, "");
		repository.registerLibraryServer(library, Repository.HOSTNAME, 0, stubName);
		libraryOperationsImpl.setCentralRepository(repository);
		log.debug("Saved server details and UDP connection details with central repository.");

		log.debug("Concordia server is up.");
		for (;;) {
			orb.run();
		}
	}

}
