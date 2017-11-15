package distributed;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is the main class you need to instances.
 */
public class Item implements Runnable, ItemRMI {

	ReentrantLock mutex;
	String[] itemPeers; // hostname
	int[] itemPorts; // host port
	String[] personPeers; // hostname
	int[] personPorts; // host port
	int me; // index into peers[]

	Registry registry;
	private double price;
	private int N1;
	ItemRMI stub;

	// Your data here

	/**
	 * Call the constructor to create a Paxos peer. The hostnames of all the
	 * Paxos peers (including this one) are in peers[]. The ports are in
	 * ports[].
	 */
	public Item(int me, int nPersons, String[] itemPeers, int[] itemPorts, String[] personPeers, int[] personPorts) {

		this.me = me;
		this.itemPeers = itemPeers;
		this.itemPorts = itemPorts;
		this.personPeers = personPeers;
		this.personPorts = personPorts;
		this.mutex = new ReentrantLock();
		this.price = 0.0;
		this.N1 = nPersons;
		try {
			System.setProperty("java.rmi.server.hostname", this.itemPeers[this.me]);
			registry = LocateRegistry.createRegistry(this.itemPorts[this.me]);
			stub = (ItemRMI) UnicastRemoteObject.exportObject(this, this.itemPorts[this.me]);
			registry.rebind("Item", stub);
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

		PersonRMI stub;
		try {
			Registry registry = LocateRegistry.getRegistry(this.personPorts[id]);
			stub = (PersonRMI) registry.lookup("Person");
			if (rmi.equals("Update"))
				stub.Update(req);
			else
				System.out.println("Wrong parameters!");
		} catch (Exception e) {
			return null;
		}
		return callReply;
	}

	public void Start() {
		Thread mythread = new Thread(this);
		mythread.start();
	}

	public Response Bid(Request req) {
		this.mutex.lock();
		if (req.price > this.price) {
			this.price = req.price;
			for (int i = 0; i < N1; i++) {
				if (i != req.id) {
					Call("Update", new Request(this.me, this.price), i);
				}
			}
			this.mutex.unlock();
			return new Response(true);
		} else {
			this.mutex.unlock();
			return new Response(false);
		}
	}

	@Override
	public void run() {
		while (true) {

		}
	}

}
