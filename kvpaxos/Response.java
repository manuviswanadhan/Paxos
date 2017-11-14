package kvpaxos;

import java.io.Serializable;

/**
 * Please fill in the data structure you use to represent the response message for each RMI call.
 * Hint: Make it more generic such that you can use it for each RMI call.
 */
public class Response implements Serializable {
    static final long serialVersionUID=22L;
    // your data here
    int val;
    boolean isOkay;

    public Response(int val, boolean isOkay) {
        this.val = val;
        this.isOkay = isOkay;
    }


    // Your constructor and methods here
}
