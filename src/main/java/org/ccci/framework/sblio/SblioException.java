package org.ccci.framework.sblio;

/**
 * More or less a RuntimeException wrapper for SiebelExceptions, but
 * can be used to wrap other exceptions that may occur in {@link DssDataBean} methods
 * 
 * @author Matt Drees
 */
public class SblioException extends RuntimeException
{

    public SblioException(String message)
    {
        super(message);
    }

    public SblioException(String message, Throwable cause)
    {
        super(message, cause);
    }

    private static final long serialVersionUID = 1L;
}
