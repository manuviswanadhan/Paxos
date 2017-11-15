package distributed;

import java.util.Random;

import org.junit.Test;

import distributed.Person.retStatus;

/**
 * This is a subset of entire test cases For your reference only.
 */
public class AuctionTest {

	
	private Person[] persons;
	private Item[] items;
	private retStatus isDecided(Person[] pxa, int personIndex) {
		return pxa[personIndex].Status();
	}

	private retStatus waitForDecide(Person[] pxa, int personIndex) throws Exception {
		int to = 10;
		for (int i = 0; i < 30; i++) {
			retStatus ret = isDecided(pxa, personIndex);
			if (ret.state != State.Pending) {
				return ret;
			}
			try {
				Thread.sleep(to);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (to < 1000) {
				to = to * 2;
			}
		}

		throw new Exception("time out error!");

	}

	private void init(int nPersons, int nItems, double eps, double[][] weight, double[] price) {
		String host = "127.0.0.1";
		String[] personPeers = new String[nPersons];
		int[] personPorts = new int[nPersons];
		persons = new Person[nPersons];
		for (int i = 0; i < nPersons; i++) {
			personPorts[i] = 1100 + i;
			personPeers[i] = host;
		}
		
		String[] itemPeers = new String[nItems];
		int[] itemPorts = new int[nItems]; 
		items = new Item[nItems];
		for (int i = 0; i < nItems; i++) {
			itemPorts[i] = 1200 + i;
			itemPeers[i] = host;
		}
		for (int i = 0; i < nPersons; i++) {
			persons[i] = new Person(i, itemPeers, itemPorts, personPeers, personPorts, nItems, eps, weight[i], price);
		}
		
		for (int i = 0; i < nItems; i++) {
			items[i] = new Item(i, nPersons, itemPeers, itemPorts, personPeers, personPorts);
		}
		
	}
//	private Person[] initPerson(int nperson, double eps, double[][] weight) {
//		String host = "127.0.0.1";
//		String[] peers = new String[nperson];
//		int[] ports = new int[nperson];
//		Person[] pxa = new Person[nperson];
//		for (int i = 0; i < nperson; i++) {
//			ports[i] = 1100 + i;
//			peers[i] = host;
//		}
//		for (int i = 0; i < nperson; i++) {
//			pxa[i] = new Person(i, peers, ports, nperson, eps, weight[i]);
//		}
//		return pxa;
//	}
//	
//	private Item[] initItem(int nItems) {
//		String host = "127.0.0.1";
//		String[] peers = new String[nItems];
//		int[] ports = new int[nItems];
//		Item[] pxa = new Item[nItems];
//		for (int i = 0; i < nItems; i++) {
//			ports[i] = 1200 + i;
//			peers[i] = host;
//		}
//		for (int i = 0; i < nItems; i++) {
//			pxa[i] = new Item(i, peers, ports);
//		}
//		return pxa;
//	}

	@Test
	public void TestBasic() {
		Random rand = new Random();
		final int N1 = 5;
		final int N2 = 5;
		final double eps = 1.0 / (2 * N1);
//		final double eps = 0.0;
		int n_set = N1;
		int[] l_set = new int[N1];
		double[][] weight = {
				{5, 1, 1, 1, 1},
				{1, 5, 1, 1, 1},
				{1, 1, 5, 1, 1},
				{1, 1, 1, 5, 1},
				{1, 1, 1, 1, 5}
		};
		for (int i = 0; i < N1; i++) {
			for (int j = 0; j < N2; j++) {
				weight[i][j] = rand.nextDouble() * 5.0;
			}
		}
		double[] price = new double[N2];
		int[] parent = new int[N2];

		for (int i = 0; i < N2; i++) {
			price[i] = 0.0;
			parent[i] = -1;
		}
		init(N1, N2, eps, weight, price);

		for (int i = 0; i < N1; i++) {
			l_set[i] = i;
		}
		
		for(int i = 0; i < N2; i++) {
			items[i].Start();
		}
		for(int i = 0; i < N1; i++) {
			persons[i].Start();
		}
		while(true) {
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
//
//		while (n_set > 0) {
//			int currPerson = l_set[n_set - 1];
//			ps[currPerson].Start(price);
//			retStatus ret = null;
//			try {
//				ret = waitForDecide(ps, currPerson);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			if(ret.state == State.Terminated) {
//				n_set--;
//			} else {
//				if(parent[ret.objectIdx] == -1) {
//					n_set--;
//				} else {
//					l_set[n_set - 1] = parent[ret.objectIdx];
//				}
//				parent[ret.objectIdx] = currPerson;
//				price[ret.objectIdx] = ret.price;
//			}
//		}
//		for(int i = 0; i < N2; i++) {
//			System.out.println(i + ": " + parent[i]);
//			System.out.println(i + ": " + price[i]);
//		}

	}

}
