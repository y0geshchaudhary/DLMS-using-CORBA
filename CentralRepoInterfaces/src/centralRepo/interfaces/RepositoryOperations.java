package centralRepo.interfaces;


/**
* centralRepo/interfaces/RepositoryOperations.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from CentralRepoInterfaces.idl
* Sunday, March 3, 2019 5:08:25 AM IST
*/

public interface RepositoryOperations 
{
  boolean registerLibraryServer (String library, String hostname, int port, String stubName);
  centralRepo.interfaces.ServerDetail getServerDetails (String libId);
} // interface RepositoryOperations