package kvpaxos;

import paxos.Paxos;
import paxos.State;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

// You are allowed to call Paxos.Status to check if agreement was made.

public class Server implements KVPaxosRMI {

    ReentrantLock mutex;
    Registry registry;
    Paxos px;
    int me;
    int seq;
    Map<String, Integer> kvstore;

    String[] servers;
    int[] ports;
    KVPaxosRMI stub;

    // Your definitions here


    public Server(String[] servers, int[] ports, int me){
        this.me = me;
        this.servers = servers;
        this.ports = ports;
        this.mutex = new ReentrantLock();
        this.px = new Paxos(me, servers, ports);
        // Your initialization code here
        this.kvstore = new HashMap<>();
        this.seq = 0;


        try{
            System.setProperty("java.rmi.server.hostname", this.servers[this.me]);
            registry = LocateRegistry.getRegistry(this.ports[this.me]);
            stub = (KVPaxosRMI) UnicastRemoteObject.exportObject(this, this.ports[this.me]);
            registry.rebind("KVPaxos", stub);
        } catch(Exception e){
            e.printStackTrace();
        }
    }


    // RMI handlers
    public Response Get(Request req){
        // Your code here
        this.mutex.lock();
        Op xop = new Op("Get", req.opid, req.key, req.val);
        Integer val = sync(xop);
        this.mutex.unlock();
        return new Response(val, true);
    }

    private Integer sync(Op xop) {
        int seq = this.seq;
        int wait = 10;
        while(true)
        {
            Paxos.retStatus ret = this.px.Status(seq);
            if(ret.state == State.Decided)
            {
                Op retop = (Op)ret.v;
                this.px.Done(seq);
                this.seq++;
                wait = 10;
                this.apply(retop);
                if (retop.ClientSeq == xop.ClientSeq) // our out value matches
                {
                    if(xop.op.equals("Get"))
                        return this.kvstore.get(xop.key);
                    return 1;
                }

            }
            else
            {
                this.px.Start(seq,xop);
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                wait = wait*2;
            }

        }
    }

    private void apply(Op xop) {
        if (xop.op.equals("Put"))
            this.kvstore.put(xop.key, xop.value);
    }


    public Response Put(Request req){
        // Your code here
        this.mutex.lock();
        Op xop = new Op("Put", req.opid, req.key, req.val);
        Integer val = sync(xop);
        this.mutex.unlock();
        return new Response(val, true);
    }


}
