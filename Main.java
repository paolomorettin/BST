
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Main {

    public static boolean stressTest(int iterations, int nOps, int nThreads, int minValue, int maxValue) {
	System.out.println("Stress");
	for (int i = 0; i < iterations; i ++) {
	    System.out.println("Stress " + i + ": Iteration started!");
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
	    System.out.println("Stress " + i +": Iteration completed!");
	    if (!tree.verify()) {
		System.out.println("Verify returned false:");
		System.out.println(tree.prettyPrint());
		return false;
	    } else
		System.out.println("Stress " + i +": Iteration verified!");
	}
	return true;
    }

    public static boolean correctnessTest(int iterations, int minValue, int maxValue) {
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

	    ListInserter ti = new ListInserter(tree,inserts);
	    ListDeleter td = new ListDeleter(tree,deletes);

	    ti.start();
	    td.start();

	    try {
		ti.join();
		td.join();

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

	int iterations = 10000;
	int nOps = 1000;
	int nThreads = 20;
	int minValue = -5;
	int maxValue = 5;

	if (!correctnessTest(iterations, minValue, maxValue) || !stressTest(iterations,nOps,nThreads,minValue,maxValue)) {
	    System.out.println("Fuck!");
	    System.exit(1);
	} 
	System.out.println("Oh yeah!");	    
    }
}

class RandomInserter extends Thread {

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
	    //System.out.println("Thread:"+this.getId()+" Insert(" + el + ") - " + tree.prettyPrint());
	    try {
		tree.insert(el);	    
	    } catch (NullPointerException e) {
		System.out.println("WTF NullPointer");
		System.exit(1);
	    }
	}
    }
}

class RandomDeleter extends Thread {

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
		System.out.println("WTF NullPointer");
		System.exit(1);
	    }
	}
    }
}

class ListInserter extends Thread {
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
		System.out.println("WTF NullPointer");
		System.exit(1);
	    }
	}
    }
}

class ListDeleter extends Thread {
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
		System.out.println("WTF NullPointer");
		System.exit(1);
	    }
	}
    }
}
