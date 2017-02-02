package org.wso2.carbon.andes.core;


/**
 * Exception type generic to all andes core level exceptions
 */
public class AndesException extends Exception {

    private String faultCode;
    private String faultString;

    public AndesException() {
        super();
    }

    public AndesException(String faultString, String faultCode) {
        this.faultCode = faultCode;
        this.faultString = faultString;
    }

    public AndesException(String message) {
        super(message);
    }

    public AndesException(String message, Throwable cause) {
        super(message, cause);
    }

    public AndesException(Throwable cause) {
        super(cause);
    }

    public String getFaultCode() {
        return faultCode;
    }

    public String getFaultString() {
        return faultString;
    }

}
