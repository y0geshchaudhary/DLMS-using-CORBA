package server.interfaces;

/**
* server/interfaces/GeneralExceptionHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from ServerInterfaces.idl
* Friday, March 8, 2019 1:13:06 PM IST
*/

public final class GeneralExceptionHolder implements org.omg.CORBA.portable.Streamable
{
  public server.interfaces.GeneralException value = null;

  public GeneralExceptionHolder ()
  {
  }

  public GeneralExceptionHolder (server.interfaces.GeneralException initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = server.interfaces.GeneralExceptionHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    server.interfaces.GeneralExceptionHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return server.interfaces.GeneralExceptionHelper.type ();
  }

}
