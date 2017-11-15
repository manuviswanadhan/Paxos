package distributed;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is the main class you need to implement paxos instances.
 */
public class Item implements Runnable{

    ReentrantLock mutex;
    String[] personPeers; // hostname
    int[] personPorts; // host port
    int me; // index into peers[]

    Registry registry;
    private double price;
    private double owner;
    private int N1;
    

    
    // Your data here


    /**
     * Call the constructor to create a Paxos peer.
     * The hostnames of all the Paxos peers (including this one)
     * are in peers[]. The ports are in ports[].
     */
    public Item(int me, int nPersons, String[] peers, int[] ports){

        this.me = me;
        this.personPeers = peers;
        this.personPorts = ports;
        this.mutex = new ReentrantLock();
        this.price = 0.0;
        this.owner = -1;
        this.N1 = nPersons;
    }


    /**
     * Call() sends an RMI to the RMI handler on server with
     * arguments rmi name, request message, and server id. It
     * waits for the reply and return a response message if
     * the server responded, and return null if Call() was not
     * be able to contact the server.
     *
     * You should assume that Call() will time out and return
     * null after a while if it doesn't get a reply from the server.
     *
     * Please use Call() to send all RMIs and please don't change
     * this function.
     */
    public Response Call(String rmi, Request req, int id){
        Response callReply = null;

        AuctionRMI stub;
        try{
            Registry registry=LocateRegistry.getRegistry(this.personPorts[id]);
            stub=(AuctionRMI) registry.lookup("Paxos");
            if(rmi.equals("Bid"))
                callReply = stub.Bid(req);
            else if(rmi.equals("Response"))
                callReply = stub.Response(req);
            else
                System.out.println("Wrong parameters!");
        } catch(Exception e){
            return null;
        }
        return callReply;
    }

    public void Start(){
        Thread mythread = new Thread(this);
        mythread.start();
    }


    @Override
    public void run(){
    }



    /**
     * the application wants to know whether this
     * peer thinks an instance has been decided,
     * and if so what the agreed value is. Status()
     * should just inspect the local peer state;
     * it should not contact other Paxos peers.
     */
//    public retStatus Status(){
//        return ret;
//    }
//
//    /**
//     * helper class for Status() return
//     */
//    public class retStatus{
//        public State state;
//        public int objectIdx;
//        public double price;
//
//        public retStatus(State state, int objectIdx, double price){
//            this.state = state;
//            this.objectIdx = objectIdx;
//            this.price = price;
//        }
//    }


}
