package distributed;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is the main class you need to implement paxos instances.
 */
public class Person implements Runnable, PersonRMI {

	ReentrantLock mutex;
	String[] itemPeers; // hostname
	int[] itemPorts; // host port
	String[] personPeers; // hostname
	int[] personPorts; // host port
	int me; // index into peers[]
	int assignedId; // object assigned to the person

	Registry registry;
	private int N2;
	private double[] weight;
	private double[] prices;
	private double eps;
	private retStatus ret;
	PersonRMI stub;

	// Your data here

	/**
	 * Call the constructor to create a Paxos peer. The hostnames of all the
	 * Paxos peers (including this one) are in peers[]. The ports are in
	 * ports[].
	 * 
	 * @param eps
	 */
	public Person(int me, String[] itemPeers, int[] itemPorts, String[] personPeers, int[] personPorts, int N2,
			double eps, double[] weight, double[] prices) {

		this.me = me;
		this.itemPeers = itemPeers;
		this.itemPorts = itemPorts;
		this.personPeers = personPeers;
		this.personPorts = personPorts;
		this.mutex = new ReentrantLock();
		this.N2 = N2;
		this.weight = weight;
		this.eps = eps;
		this.prices = prices;
		this.assignedId = -1;
		// register peers, do not modify this part
		try {
			System.setProperty("java.rmi.server.hostname", this.personPeers[this.me]);
			registry = LocateRegistry.createRegistry(this.personPorts[this.me]);
			stub = (PersonRMI) UnicastRemoteObject.exportObject(this, this.personPorts[this.me]);
			registry.rebind("Person", stub);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Call() sends an RMI to the RMI handler on server with arguments rmi name,
	 * request message, and server id. It waits for the reply and return a
	 * response message if the server responded, and return null if Call() was
	 * not be able to contact the server.
	 *
	 * You should assume that Call() will time out and return null after a while
	 * if it doesn't get a reply from the server.
	 *
	 * Please use Call() to send all RMIs and please don't change this function.
	 */
	public Response Call(String rmi, Request req, int id) {
		Response callReply = null;
		ItemRMI stub;
		try {
			Registry registry = LocateRegistry.getRegistry(this.itemPorts[id]);
			stub = (ItemRMI) registry.lookup("Item");
			if (rmi.equals("Bid")) {
				callReply = stub.Bid(req);
			} else
				System.out.println("Wrong parameters!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return callReply;
	}

	/**
	 * The application wants Paxos to start agreement on instance seq, with
	 * proposed value v. Start() should start a new thread to run Paxos on
	 * instance seq. Multiple instances can be run concurrently.
	 *
	 * Hint: You may start a thread using the runnable interface of Paxos
	 * object. One Paxos object may have multiple instances, each instance
	 * corresponds to one proposed value/command. Java does not support passing
	 * arguments to a thread, so you may reset seq and v in Paxos object before
	 * starting a new thread. There is one issue that variable may change before
	 * the new thread actually reads it. Test won't fail in this case.
	 *
	 * Start() just starts a new thread to initialize the agreement. The
	 * application will call Status() to find out if/when agreement is reached.
	 */
	public void Start() {
		// Your code here

		this.ret = new retStatus(State.Pending, -1, -1);
		Thread mythread = new Thread(this);
		mythread.start();

	}

	@Override
	public void run() {
		while (ret.state != State.Terminated) {
			if (ret.state == State.Pending) {
				bid();
			} else {
				try {
					Thread.sleep(30);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println(this.me + " terminated!");
	}

	void bid() {
		double max = 0.0;
		int maxIndex = -1;
		for (int i = 0; i < N2; i++) {
			if (weight[i] - prices[i] > max) {
				max = weight[i] - prices[i];
				maxIndex = i;
			}
		}
		if (max == 0.0) {
			ret.state = State.Terminated;
		} else {
			double max2 = 0.0;
			for (int i = 0; i < N2; i++) {
				if (weight[i] - prices[i] > max2 && i != maxIndex) {
					max2 = weight[i] - prices[i];
				}
			}
			double price = prices[maxIndex] + max - max2 + eps;
			Response rsp = Call("Bid", new Request(this.me, price), maxIndex);
			if (rsp.isAccept) {
				ret.state = State.Decided;
				ret.price = price;
				this.assignedId = maxIndex;
				System.out.println("person id:" + this.me + ", object id: " + this.assignedId + ", price: " + price);
			} 
			
			// ret.price = prices[maxIndex] + max - max2 + eps;
			// ret.objectIdx = maxIndex;
			// ret.state = State.Decided;
		}
	}

	/**
	 * the application wants to know whether this peer thinks an instance has
	 * been decided, and if so what the agreed value is. Status() should just
	 * inspect the local peer state; it should not contact other Paxos peers.
	 */
	public retStatus Status() {
		return ret;
	}

	/**
	 * helper class for Status() return
	 */
	public class retStatus {
		public State state;
		public int objectIdx;
		public double price;

		public retStatus(State state, int objectIdx, double price) {
			this.state = state;
			this.objectIdx = objectIdx;
			this.price = price;
		}
	}

	@Override
	public void Update(Request req) {
		prices[req.id] = req.price;
		this.mutex.lock();
		if(this.assignedId == req.id){
			ret.state = State.Pending;
			this.assignedId = -1;
		}
		this.mutex.unlock();
	}

}
