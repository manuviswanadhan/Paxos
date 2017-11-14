package paxos;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is the main class you need to implement paxos instances.
 */
public class Paxos implements PaxosRMI, Runnable{

    ReentrantLock mutex;
    String[] peers; // hostname
    int[] ports; // host port
    int me; // index into peers[]

    Registry registry;
    PaxosRMI stub;

    int seq;
    Object value;

    AtomicBoolean dead;// for testing
    AtomicBoolean unreliable;// for testing
    private int maxSeqSeen;

    Map<Integer, Object> instanceMap;
    Map<Integer, Integer> highestPrepProposal;
    Map<Integer, Integer> highestAcceptProposal;
    Map<Integer, Object> highestAcceptVal;
    Map<Integer, Integer> doneSeq;
    // Your data here


    /**
     * Call the constructor to create a Paxos peer.
     * The hostnames of all the Paxos peers (including this one)
     * are in peers[]. The ports are in ports[].
     */
    public Paxos(int me, String[] peers, int[] ports){

        this.maxSeqSeen = -1;
        this.me = me;
        this.peers = peers;
        this.ports = ports;
        this.mutex = new ReentrantLock();
        this.dead = new AtomicBoolean(false);
        this.unreliable = new AtomicBoolean(false);

        this.instanceMap = new HashMap<>();
        this.highestPrepProposal = new HashMap<>();
        this.highestAcceptProposal = new HashMap<>();
        this.highestAcceptVal = new HashMap<>();
        this.doneSeq = new HashMap<>();


        // Your initialization code here


        // register peers, do not modify this part
        try{
            System.setProperty("java.rmi.server.hostname", this.peers[this.me]);
            registry = LocateRegistry.createRegistry(this.ports[this.me]);
            stub = (PaxosRMI) UnicastRemoteObject.exportObject(this, this.ports[this.me]);
            registry.rebind("Paxos", stub);
        } catch(Exception e){
            e.printStackTrace();
        }
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

        PaxosRMI stub;
        try{
            Registry registry=LocateRegistry.getRegistry(this.ports[id]);
            stub=(PaxosRMI) registry.lookup("Paxos");
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
    public void Start(int seq, Object value){
        // Your code here

        this.seq = seq;
        this.value = value;
        Thread mythread = new Thread(this, ""+seq);
        mythread.start();


    }

    private void updateMaxSqSeen(int seq) {
        if (seq > this.maxSeqSeen)
            this.maxSeqSeen = seq;
    }

    @Override
    public void run(){

        //Your code here

        //        if seq < px.Min() {
//            return
//        }
//
//        px.mu.Lock()
//        px.updateMaxSeqSeen(seq)
//        px.mu.Unlock()
//
//        go px.propose(seq, v)

        int seq = this.seq;
        Object val = this.value;

        this.mutex.lock();
        this.updateMaxSqSeen(seq);
        this.mutex.unlock();

        this.propose(seq, val);
    }

    private void propose(int seq, Object val) {
        while(!this.isDecided(seq))
        {
            int num = this.chooseProposalNum(seq);
            Object val_selected = sendPrepareToAll(num, seq, val);
            if(val_selected == null) {
                /*Random rnd = new Random();
                try {
                    Thread.sleep(rnd.nextInt(100));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } */
                continue;
            }
            if(!sendAcceptToAll(seq, num,val_selected))
                continue;
            this.sendDecidedToAll(seq, val_selected);

        }
    }

    private void sendDecidedToAll(int seq, Object val) {
        for(int i = 0; i < this.peers.length; i++) {
            this.decidedUtil(new Request(seq, -1, val, i));
        }
    }

    private void decidedUtil(Request request) {
        this.mutex.lock();
        if(this.me == request.peerIdx) {
            highestAcceptVal.put(Integer.valueOf(request.seq), request.val);
            instanceMap.put(request.seq, request.val);
            this.mutex.unlock();
        } else {
            int doneIns = (this.doneSeq.containsKey(this.me))?this.doneSeq.get(this.me):0;
            this.mutex.unlock();
            Call("Decide", new Request(request.seq, doneIns, request.val, this.me), request.peerIdx);
        }
    }


    public Response Decide(Request req){
        //propnum used for doneins
        this.mutex.lock();
        this.highestAcceptVal.put(Integer.valueOf(req.seq), req.val);
        this.instanceMap.put(req.seq, req.val);
        if(!this.doneSeq.containsKey(Integer.valueOf(req.peerIdx)) || this.doneSeq.get(Integer.valueOf(req.peerIdx)).intValue() < req.propNum ) {
            this.doneSeq.put(Integer.valueOf(req.peerIdx), req.propNum);
        }

        this.mutex.unlock();
        return null;

    }

    private boolean sendAcceptToAll(int seq, int num, Object val_selected) {
        int cntOkay = 0;
        for(int i=0; i < this.peers.length; i++)
        {
            if(this.acceptUtil(new Request(seq, num, val_selected, i)))
                cntOkay++;
        }
        if ( cntOkay > this.peers.length/2 )
            return true;
        return false;
    }

    private boolean acceptUtil(Request req) {
        if (req.peerIdx == this.me) {
            return this.acceptHandler(req.seq, req.propNum, req.val);
        }
        Response rsp = Call("Accept", req, req.peerIdx);
        return rsp == null ?false: rsp.isOkay;
    }

    private boolean acceptHandler(int seq, int propNum, Object val) {
        this.mutex.lock();
        this.updateMaxSqSeen(seq);
        boolean rsp;
        if (!highestPrepProposal.containsKey(Integer.valueOf(seq)) || highestPrepProposal.get(Integer.valueOf(seq)) <= propNum)
        {
            highestPrepProposal.put(Integer.valueOf(seq), Integer.valueOf(propNum));
            highestAcceptProposal.put(Integer.valueOf(seq), Integer.valueOf(propNum));
            highestAcceptVal.put(Integer.valueOf(seq), val);
            rsp = true;
        }
        else
            rsp = false;
        this.mutex.unlock();
        return rsp;
    }

    private boolean isDecided(int seq) {
        return Status(seq).state == State.Decided;
    }

    private int chooseProposalNum(int seq) {
        this.mutex.lock();
        int n;
        if(!this.highestPrepProposal.containsKey(seq))
            n = -1;
        else
            n = this.highestPrepProposal.get(Integer.valueOf(seq));
        this.mutex.unlock();
        return n+1;
    }

    private Object sendPrepareToAll(int num, int seq, Object val) {
        int cntOkay = 0, maxNa = 0;
        Object maxVal = null;
        for(int i=0; i< this.peers.length; i++)
        {
            Response rsp = prepareUtil( new Request(seq, num, val, i));
            if(rsp != null) {
                if (rsp.isOkay) {
                    if ((rsp.val != null) && (rsp.propNum > maxNa)) {
                        maxNa = rsp.propNum;
                        maxVal = rsp.val;
                    }
                    cntOkay++;
                } else {
                    if (this.updateProposalNum(seq, rsp.propNum))
                        return null;  // check
                    /*Random rand = new Random();
                    try {
                        Thread.sleep(rand.nextInt(100));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
                }
            }
        }
        if (cntOkay > this.peers.length/2)
        {
            if (maxNa == 0)
                maxVal = val; // no promise from acceptors

            return maxVal ;
        }
        return null;

    }

    private boolean updateProposalNum(int seq, int propNum) {
        if(!highestPrepProposal.containsKey(Integer.valueOf(seq)) || propNum > highestPrepProposal.get(Integer.valueOf(seq)).intValue())
        {
            highestPrepProposal.put(Integer.valueOf(seq), propNum);
            return true;
        }
        return false;
    }

    // RMI handler
    public Response Prepare(Request req){
        // your code here
        return prepareHandler(req.seq, req.propNum);
    }

    public Response prepareUtil (Request req) {
        if (req.peerIdx == this.me)
            return this.prepareHandler(req.seq, req.propNum);
        return Call("Prepare", req, req.peerIdx);
    }

    private Response prepareHandler(int seq, int propNum) {
        this.updateMaxSqSeen(seq);
        this.mutex.lock();
        Response rsp;
        if (!highestPrepProposal.containsKey(Integer.valueOf(seq)) || highestPrepProposal.get(Integer.valueOf(seq)) < propNum)
        {
            highestPrepProposal.put(Integer.valueOf(seq), Integer.valueOf(propNum));
            if(highestAcceptVal.get(Integer.valueOf(seq)) == null)
                rsp = new Response(-1, highestAcceptVal.get(Integer.valueOf(seq)), true);
            else
                rsp = new Response(highestAcceptProposal.get(Integer.valueOf(seq)), highestAcceptVal.get(Integer.valueOf(seq)), true);
        }
        else
            rsp = new Response(highestPrepProposal.get(Integer.valueOf(seq)), null, false);
        this.mutex.unlock();
        return rsp;
    }

    public Response Accept(Request req){
        return new Response(0, 0, this.acceptHandler(req.seq, req.propNum, req.val));

    }

    /**
     * The application on this machine is done with
     * all instances <= seq.
     *
     * see the comments for Min() for more explanation.
     */
    public void Done(int seq) {
        // Your code here
        this.mutex.lock();
        if (!this.doneSeq.containsKey(this.me) || this.doneSeq.get(this.me) < seq) {
            this.doneSeq.put(this.me, seq);
        }

        this.mutex.unlock();
    }


    /**
     * The application wants to know the
     * highest instance sequence known to
     * this peer.
     */
    public int Max(){
        // Your code here
        this.mutex.lock();
        int n = this.maxSeqSeen;
        this.mutex.unlock();
        return n;
    }

    /**
     * Min() should return one more than the minimum among z_i,
     * where z_i is the highest number ever passed
     * to Done() on peer i. A peers z_i is -1 if it has
     * never called Done().

     * Paxos is required to have forgotten all information
     * about any instances it knows that are < Min().
     * The point is to free up memory in long-running
     * Paxos-based servers.

     * Paxos peers need to exchange their highest Done()
     * arguments in order to implement Min(). These
     * exchanges can be piggybacked on ordinary Paxos
     * agreement protocol messages, so it is OK if one
     * peers Min does not reflect another Peers Done()
     * until after the next instance is agreed to.

     * The fact that Min() is defined as a minimum over
     * all Paxos peers means that Min() cannot increase until
     * all peers have been heard from. So if a peer is dead
     * or unreachable, other peers Min()s will not increase
     * even if all reachable peers call Done. The reason for
     * this is that when the unreachable peer comes back to
     * life, it will need to catch up on instances that it
     * missed -- the other peers therefore cannot forget these
     * instances.
     */
    public int Min(){
        // Your code here
        this.mutex.lock();
        int n = this.doMemShrink()+1;
        this.mutex.unlock();
        return n;
    }

    private int doMemShrink() {
        int n = this.doneSeq.containsKey(Integer.valueOf(this.me))?this.doneSeq.get(Integer.valueOf(this.me)):-1;  // check later
        Iterator it = doneSeq.entrySet().iterator();
        while(it.hasNext())
        {
            Map.Entry mp = (Map.Entry)it.next();
            if (((Integer)mp.getValue()) < n)
                n = (Integer)mp.getValue();
        }
        it = this.instanceMap.entrySet().iterator();
        while(it.hasNext())
        {
            Map.Entry mp = (Map.Entry)it.next();
            if (((Integer)mp.getKey()) <= n)
            {
                it.remove();
                highestPrepProposal.remove(mp.getKey());
            }
        }
        return n;
    }


    /**
     * the application wants to know whether this
     * peer thinks an instance has been decided,
     * and if so what the agreed value is. Status()
     * should just inspect the local peer state;
     * it should not contact other Paxos peers.
     */
    public retStatus Status(int seq){
        // Your code here
        retStatus ret;
        if(seq < this.Min())
            return new retStatus(State.Forgotten,null);
        this.mutex.lock();
        if (instanceMap.containsKey(Integer.valueOf(seq)))
            ret = new retStatus(State.Decided, instanceMap.get(Integer.valueOf(seq)));
        else
            ret = new retStatus(State.Pending,null);
        this.mutex.unlock();
        return ret;
    }

    /**
     * helper class for Status() return
     */
    public class retStatus{
        public State state;
        public Object v;

        public retStatus(State state, Object v){
            this.state = state;
            this.v = v;
        }
    }

    /**
     * Tell the peer to shut itself down.
     * For testing.
     * Please don't change these four functions.
     */
    public void Kill(){
        this.dead.getAndSet(true);
        if(this.registry != null){
            try {
                UnicastRemoteObject.unexportObject(this.registry, true);
            } catch(Exception e){
                System.out.println("None reference");
            }
        }
    }

    public boolean isDead(){
        return this.dead.get();
    }

    public void setUnreliable(){
        this.unreliable.getAndSet(true);
    }

    public boolean isunreliable(){
        return this.unreliable.get();
    }


}
