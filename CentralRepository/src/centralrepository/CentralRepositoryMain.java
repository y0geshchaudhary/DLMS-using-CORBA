package centralrepository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import centralRepo.interfaces.Repository;
import centralRepo.interfaces.RepositoryHelper;
import centralrepository.model.RepositoryImpl;

public class CentralRepositoryMain {

	private static final Logger log = LogManager.getLogger(CentralRepositoryMain.class);

	public static void main(String[] args) {
		log.debug("Inside main() method.");
		RepositoryImpl repositoryImpl = new RepositoryImpl();

		
		String[] arg = { "-ORBInitialPort", String.valueOf(Repository.PORT), "-ORBInitialHost localhost" };
		try {
			log.debug("Starting ORB demon.");
			// start orbd -ORBInitialPort 2000 -ORBInitialHost localhost
			Runtime.getRuntime().exec("cmd /c start orbd -ORBInitialPort "+Repository.PORT+ " -ORBInitialHost localhost");
			
			ORB orb = ORB.init(arg, null);
			POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
			rootpoa.the_POAManager().activate();

			repositoryImpl.setOrb(orb);

			org.omg.CORBA.Object obj = rootpoa.servant_to_reference(repositoryImpl);
			Repository repository = RepositoryHelper.narrow(obj);

			org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
			NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

			NameComponent path[] = ncRef.to_name(Repository.STUBNAME);
			ncRef.rebind(path, repository);

			log.debug("Central Repository is up.");
			for (;;) {
				orb.run();
			}
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}

}
