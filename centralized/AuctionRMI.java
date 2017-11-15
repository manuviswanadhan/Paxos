package centralized;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * This is the interface of Paxos RMI call. You should implement each method defined below.
 * Prepare is the RMI that proposer sends prepare request to acceptors.
 * Accept is the RMI that proposer sends accept request to acceptors.
 * Decide is the RMI that proposer broadcasts decision once consensus reaches.
 * Please don't change the interface.
 */
public interface AuctionRMI extends Remote{
    Response Bid(Request req) throws RemoteException;
    Response Response(Request req) throws RemoteException;
}
