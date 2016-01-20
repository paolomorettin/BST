
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.nio.file.*;
import java.nio.charset.Charset;
import java.io.IOException;

public class Main {

    private static void printUsage() {
	String usage = "USAGE: java Main mode [parameters]\n";
	usage += "MODES\\PARAMS:\n";
	usage += "\t- \"random\" iterations n_ops_per_thread n_threads min_value max_value\n";
	usage += "\t- \"correctness\" iterations n_threads min_value max_value\n";
	usage += "\t- \"sequential\" graph_name min_value max_value\n";
	System.out.printf(usage);
    }

    private static void sequentialTest(String graphname, int minValue, int maxValue) {
	/* this function sequentially performs (in this order):
	   - 10 random insertions
	   - 5 random deletions
	   - 5 find queries
	   printing the resulting tree and the outcome of the operation at each step,
	   then it saves the resulting tree in .DOT format.

	   the key space is [minValue,maxValue].
	*/
	BST tree = new BST();
	boolean outcome;
	int element;
	System.out.println("Sequential test:");
	System.out.println("Initial tree: " + tree.prettyPrint());
	for (int i = 0; i < 10; i++) {
	    element = ThreadLocalRandom.current().nextInt(minValue, maxValue);
	    outcome = tree.insert(element);
	    System.out.println("Insert(" + element + ") returned " + outcome);
	    System.out.println("Resulting tree: " + tree.prettyPrint());
	}
	for (int i = 0; i < 5; i++) {
	    element = ThreadLocalRandom.current().nextInt(minValue, maxValue);
	    outcome = tree.delete(element);
	    System.out.println("Delete(" + element + ") returned " + outcome);
	    System.out.println("Resulting tree: " + tree.prettyPrint());
	}
	for (int i = 0; i < 5; i++) {
	    element = ThreadLocalRandom.current().nextInt(minValue, maxValue);
	    outcome = tree.find(element);
	    System.out.println("Find(" + element + ") returned " + outcome);
	}
	    
	
	List<String> lines = Arrays.asList(tree.DOTFormat(graphname));
	Path file = Paths.get(graphname + ".dot");
	try {
	    Files.write(file, lines, Charset.forName("UTF-8"));
	} catch (IOException e) {
	    System.out.println("Something went wrong writing the .dot file");
	    System.exit(1);
	}
    }

    private static boolean randomTest(int iterations, int nOps, int nThreads, int minValue, int maxValue) {
	/* this function performs a "random test" on the tree. given:
	   - the number of iterations
	   - the number of operation performed by each thread
	   - the number of threads
	   - the minimum and maximum value of the key space (subset of int)

	   for each iteration a new tree is instantiated and the threads (divided in inserters and deleters)
	   concurrently perform the operations on the tree. when they are done, it verifies that the tree is
	   actually a BST, calling tree.verify()

	   if all the iterations are correct, returns true, otherwise returns false
	*/
	System.out.println("Random");
	for (int i = 0; i < iterations; i ++) {
	    System.out.println("Random " + i + ": Iteration started!");
	    BST tree = new BST();
	    List<Thread> threads = new ArrayList<Thread>();
	    for (int t = 0; t < nThreads; t++) {
		if (t % 2 == 0)
		    threads.add( new RandomInserter(tree, nOps, minValue, maxValue) );
		else
		    threads.add( new RandomDeleter(tree, nOps, minValue, maxValue) );
	    }

	    for (Thread t : threads)
		t.start();

	    try {
		for (Thread t : threads)
		    t.join();		
	    } catch (InterruptedException e) {
		System.out.println("Whatever..");
		return false;
	    }
	    System.out.println("Random " + i +": Iteration completed!");
	    if (!tree.verify()) {
		return false;
	    } else
		System.out.println("Random " + i +": Iteration verified!");
	}
	return true;
    }

    private static boolean correctnessTest(int iterations, int nThreads, int minValue, int maxValue) {
	/* this function performs a "correctness test" on the tree. given:
	   - the number of iterations
	   - the number of threads
	   - the minimum and maximum value of the key space (subset of int)

	   for each iteration a new tree is instantiated together with 3 sets of keys:
	   - X: those that are present in the tree before the threads start inserting/deleting
	   - I: those that will be inserted in the tree
	   - D: those that will be deleted from the tree
	   (with I intersection D empty)
	   
	   threads are divided into inserters and deleters and perform their operations concurrently.
	   when they are done, it checks that the resulting set of keys in the tree is 
	      (X \ D) + I

	   if all the iterations are correct, returns true, otherwise returns false
	*/
	System.out.println("Correctness");
	for (int i = 0; i < iterations; i ++) {
	    System.out.println("Correctness " + i + ": Iteration started!");
	    BST tree = new BST();
	    Set<Integer> X = new TreeSet<Integer>();
	    Set<Integer> I = new TreeSet<Integer>();
	    Set<Integer> D = new TreeSet<Integer>();

	    for (int j = minValue; j < maxValue + 1 ; j++) {		
		switch (ThreadLocalRandom.current().nextInt(0, 5)) {
		case 0 : X.add(j); break;
		case 1 : I.add(j); break;
		case 2 : D.add(j); break;
		case 3 : X.add(j); D.add(j); break;
		case 4 : X.add(j); I.add(j); break;
		}
	    }

	    Set<Integer> R = new TreeSet<Integer>(X); // expected set of keys: (X \ D) + I (because intersection of I and D is empty)
	    R.removeAll(D);
	    R.addAll(I);

	    for (int el : X)
		tree.insert(el);

	    List<Integer> inserts = new ArrayList<Integer>(I);
	    List<Integer> deletes = new ArrayList<Integer>(D);
	    List<Thread> threadList = new ArrayList<Thread>();

	    for (int j = 0; j < nThreads; j++) {		
		if (j % 2 == 0) {
		    Collections.shuffle(inserts);
		    threadList.add(new ListInserter(tree,inserts));
		} else {
		    Collections.shuffle(deletes);
		    threadList.add(new ListDeleter(tree,deletes));
		}
	    }

	    for (Thread t : threadList)
		t.start();

	    try {
		for (Thread t : threadList)
		    t.join();
	    } catch (InterruptedException e) {
		System.out.println("Whatever..");
		return false;
	    }
	    System.out.println("Correctness " + i +": Iteration completed!");

	    Set<Integer> resultingKeys = tree.getKeys();
	    if (!tree.verify()) {
		System.out.println("Verify returned false:");
		System.out.println(tree.prettyPrint());
		return false;
	    } else if (!resultingKeys.equals(R)) {
		System.out.println("Resulting key set is wrong:");
		System.out.println(resultingKeys);
		System.out.println(R);
		return false;		
	    } else
		System.out.println("Correctness " + i +": Iteration verified!");
	}
	return true;
    }

    public static void main (String[] args) {
	try {
	    if (args[0].equals("random")) {
		int iter = Integer.parseInt(args[1]);
		int nOps = Integer.parseInt(args[2]);
		int nThreads = Integer.parseInt(args[3]);
		int minValue = Integer.parseInt(args[4]);
		int maxValue = Integer.parseInt(args[5]);
		
		if (minValue > maxValue) {
		    System.out.println("minValue shouldn't be greater than maxValue");
		    System.exit(1);
		}

		if (randomTest(iter,nOps,nThreads,minValue,maxValue))
		    System.out.println("OK!");
		else
		    System.out.println("Not OK");
	    } else if (args[0].equals("correctness")) {
		int iter = Integer.parseInt(args[1]);
		int nThreads = Integer.parseInt(args[2]);
		int minValue = Integer.parseInt(args[3]);
		int maxValue = Integer.parseInt(args[4]);

		if (minValue > maxValue) {
		    System.out.println("minValue shouldn't be greater than maxValue");
		    System.exit(1);
		}

		if (correctnessTest(iter,nThreads,minValue,maxValue))
		    System.out.println("OK!");
		else
		    System.out.println("Not OK");
	    } else if (args[0].equals("sequential")) {
		String graphname = args[1];
		int minValue = Integer.parseInt(args[2]);
		int maxValue = Integer.parseInt(args[3]);

		if (minValue > maxValue) {
		    System.out.println("minValue shouldn't be greater than maxValue");
		    System.exit(1);
		}
		sequentialTest(graphname,minValue,maxValue);
	    }
	} catch (ArrayIndexOutOfBoundsException e) {
	    printUsage();
	    System.exit(1);
	}



	    /*
	for (int i = 0; i < 2; i++) {
	    nThreads = ThreadLocalRandom.current().nextInt(2,11); // varying the number of threads
	    minValue = ThreadLocalRandom.current().nextInt(-100,+100);
	    maxValue = minValue + ThreadLocalRandom.current().nextInt(1,50);
	    if (!correctnessTest(iterations, nThreads, minValue, maxValue) || !randomTest(iterations,nOps,nThreads,minValue,maxValue)) {
		System.out.println("Fuck!");
		System.exit(1);
	    } 
	    System.out.println("Oh yeah! x"+i);
	    }*/	    
    }
}

class RandomInserter extends Thread {
    /* this class implements a worker that inserts "nOps" random elements in a
       range [minValue,maxValue] in a given tree
    */

    private BST tree;
    int nOps;
    int minValue,maxValue;

    public RandomInserter(BST tree,int nOps, int minValue, int maxValue) {
	this.tree = tree;
	this.nOps = nOps;
	this.minValue = minValue;
	this.maxValue = maxValue;
    }

    public void run() {
	for (int i = 0; i < nOps; i++) {
	    int el = ThreadLocalRandom.current().nextInt(minValue,maxValue);
	    try {
		tree.insert(el);	    
	    } catch (NullPointerException e) {
		System.out.println("RandomInserter: NullPointerException");
		System.exit(1);
	    }
	}
    }
}

class RandomDeleter extends Thread {
    /* this class implements a worker that attempts to delete "nOps" random elements in a
       range [minValue,maxValue] in a given tree
    */

    private BST tree;
    int nOps;
    int minValue, maxValue;

    public RandomDeleter (BST tree,int nOps, int minValue, int maxValue) {
	this.tree = tree;
	this.nOps = nOps;
	this.minValue = minValue;
	this.maxValue = maxValue;
    }

    public void run() {
	for (int i = 0; i < nOps; i++) {
	    int el = ThreadLocalRandom.current().nextInt(minValue,maxValue);
	    //System.out.println("Thread:"+this.getId()+" Delete(" + el + ") - " + tree.prettyPrint());
	    try {
		tree.delete(el);
	    } catch (NullPointerException e) {
		System.out.println("RandomDeleter: NullPointerException");
		System.exit(1);
	    }
	}
    }
}

class ListInserter extends Thread {
    /* this class implements a worker that inserts a list of elements in a
       given tree
    */

    private BST tree;
    List<Integer> inserts;

    public ListInserter(BST tree, List<Integer> inserts) {
	this.tree = tree;
	this.inserts = inserts;
    }

    public void run() {
	for (int el : inserts) {
	    try {
		tree.insert(el);	    
	    } catch (NullPointerException e) {
		System.out.println("ListInserter: NullPointerException");
		System.exit(1);
	    }
	}
    }
}

class ListDeleter extends Thread {
    /* this class implements a worker that deletes a list of elements in a
       given tree
    */

    private BST tree;
    List<Integer> deletes;

    public ListDeleter(BST tree, List<Integer> deletes) {
	this.tree = tree;
	this.deletes = deletes;
    }

    public void run() {
	for (int el : deletes) {
	    try {
		tree.delete(el);	    
	    } catch (NullPointerException e) {
		System.out.println("ListDeleter: NullPointerException");
		System.exit(1);
	    }
	}
    }
}
