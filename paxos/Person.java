package paxos;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is the main class you need to implement paxos instances.
 */
public class Person implements Runnable{

    ReentrantLock mutex;
    String[] peers; // hostname
    int[] ports; // host port
    int me; // index into peers[]

    Registry registry;
    private int N2;
    private double[] weight;
    private double[] prices;
    private double eps;
    private retStatus ret;

    
    // Your data here


    /**
     * Call the constructor to create a Paxos peer.
     * The hostnames of all the Paxos peers (including this one)
     * are in peers[]. The ports are in ports[].
     */
    public Person(int me, String[] peers, int[] ports, int N2, double eps, double[] weight){

        this.me = me;
        this.peers = peers;
        this.ports = ports;
        this.mutex = new ReentrantLock();
        
        this.N2 = N2;
        this.weight = weight;
        this.eps = eps;
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
            Registry registry=LocateRegistry.getRegistry(this.ports[id]);
            stub=(AuctionRMI) registry.lookup("Paxos");
            if(rmi.equals("Prepare"))
                callReply = stub.Prepare(req);
            else if(rmi.equals("Accept"))
                callReply = stub.Accept(req);
            else if(rmi.equals("Decide"))
                callReply = stub.Decide(req);
            else
                System.out.println("Wrong parameters!");
        } catch(Exception e){
            return null;
        }
        return callReply;
    }


    /**
     * The application wants Paxos to start agreement on instance seq,
     * with proposed value v. Start() should start a new thread to run
     * Paxos on instance seq. Multiple instances can be run concurrently.
     *
     * Hint: You may start a thread using the runnable interface of
     * Paxos object. One Paxos object may have multiple instances, each
     * instance corresponds to one proposed value/command. Java does not
     * support passing arguments to a thread, so you may reset seq and v
     * in Paxos object before starting a new thread. There is one issue
     * that variable may change before the new thread actually reads it.
     * Test won't fail in this case.
     *
     * Start() just starts a new thread to initialize the agreement.
     * The application will call Status() to find out if/when agreement
     * is reached.
     */
    public void Start(double[] prices){
        // Your code here

        this.prices = prices;
        this.ret = new retStatus(State.Pending, -1, -1);
        Thread mythread = new Thread(this);
        mythread.start();


    }


    @Override
    public void run(){
    	double max = 0.0;
    	int maxIndex = -1;
    	for(int i = 0; i < N2; i++) {
    		if(weight[i] - prices[i] > max) {
    			max = weight[i] - prices[i];
    			maxIndex = i;
    		}
    	}
    	if(max == 0.0) {
    		ret.state = State.Terminated;
    	} else {
    		double max2 = 0.0;
    		for(int i = 0; i < N2; i++) {
        		if(weight[i] - prices[i] > max2 && i != maxIndex) {
        			max2 = weight[i] - prices[i];
        		}
        	}
    		ret.price = prices[maxIndex] + max - max2 + eps;
    		ret.objectIdx = maxIndex;
    		ret.state = State.Decided;
    	}
    }



    /**
     * the application wants to know whether this
     * peer thinks an instance has been decided,
     * and if so what the agreed value is. Status()
     * should just inspect the local peer state;
     * it should not contact other Paxos peers.
     */
    public retStatus Status(){
        return ret;
    }

    /**
     * helper class for Status() return
     */
    public class retStatus{
        public State state;
        public int objectIdx;
        public double price;

        public retStatus(State state, int objectIdx, double price){
            this.state = state;
            this.objectIdx = objectIdx;
            this.price = price;
        }
    }


}
