package server.interfaces;


/**
* server/interfaces/BooksHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from ServerInterfaces.idl
* Friday, March 8, 2019 1:13:06 PM IST
*/

public final class BooksHolder implements org.omg.CORBA.portable.Streamable
{
  public server.interfaces.Book value[] = null;

  public BooksHolder ()
  {
  }

  public BooksHolder (server.interfaces.Book[] initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = server.interfaces.BooksHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    server.interfaces.BooksHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return server.interfaces.BooksHelper.type ();
  }

}
