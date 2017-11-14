package kvpaxos;
import java.io.Serializable;

/**
 * Please fill in the data structure you use to represent the request message for each RMI call.
 * Hint: Make it more generic such that you can use it for each RMI call.
 * Hint: Easier to make each variable public
 */
public class Request implements Serializable {
    static final long serialVersionUID=11L;
    Integer opid;
    String key;
    Integer val;


    public Request(Integer opid, String key) {
        this.opid = opid;
        this.key = key;
    }

    public Request(Integer opid, String key, Integer val) {
        this.opid = opid;
        this.key = key;
        this.val = val;
    }
}
