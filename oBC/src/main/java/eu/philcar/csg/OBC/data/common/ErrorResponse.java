package eu.philcar.csg.OBC.data.common;

//import org.apache.commons.lang3.builder.EqualsBuilder;
//import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ErrorResponse extends Throwable {

    public enum ErrorType{
        CUSTOM,
        NO_NETWORK,
        EMPTY,
        SERVER_TIMEOUT,
        CONVERSION,
        HTTP,
        UNEXPECTED
    }

    public ErrorType errorType;
    public String rawMessage;
    public Integer httpStatus;
    public Throwable error;

    public ErrorResponse(){}

    public ErrorResponse(ErrorType errorType){
        this.errorType = errorType;
    }
    public ErrorResponse(ErrorType errorType, Throwable error){
        this.errorType = errorType;
        this.error =error;
    }

    /*@Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) {
            return false;
        }
        ErrorResponse rhs = (ErrorResponse) obj;
        return new EqualsBuilder()
//                .appendSuper(super.equals(obj))
                .append(errorType, rhs.errorType)
                .append(httpStatus, rhs.httpStatus)
                .append(rawMessage, rhs.rawMessage)
                .isEquals();
    }

    @Override
    public int hashCode() {
        super.hashCode()
        // you pick a hard-coded, randomly chosen, non-zero, odd number
        // ideally different for each class
        return new HashCodeBuilder(17, 37)
                .append(errorType)
                .append(httpStatus)
                .append(rawMessage)
                .toHashCode();
    }*/

}
