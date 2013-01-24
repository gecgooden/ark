/*

 * DAOException.java

 *

 * Created on October 28, 2002, 2:51 PM

 */



package neuragenix.dao.exception;



/**

 *

 * @author  Administrator

 */

public class DAOSQLRollbackException extends java.lang.Exception {

    

    /**

     * Creates a new instance of <code>DAOException</code> without detail message.

     */

    public DAOSQLRollbackException() {

    }

    

    

    /**

     * Constructs an instance of <code>DAOException</code> with the specified detail message.

     * @param msg the detail message.

     */

    public DAOSQLRollbackException(String msg) {

        super(msg);

    }

}
