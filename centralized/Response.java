package centralized;
import java.io.Serializable;

/**
 * Please fill in the data structure you use to represent the response message for each RMI call.
 * Hint: You may need a boolean variable to indicate ack of acceptors and also you may need proposal number and value.
 * Hint: Make it more generic such that you can use it for each RMI call.
 */
public class Response implements Serializable {
    static final long serialVersionUID=2L;
    // your data here
    public int propNum;
    public Object val;
    public boolean isOkay;

    // Your constructor and methods here


    public Response(int propNum, Object val, boolean isOkay) {
        this.propNum = propNum;
        this.val = val;
        this.isOkay = isOkay;
    }
}
