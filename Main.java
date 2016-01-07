
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Main {
    public static void main (String[] args) {
	int iterations = 10000;
	int nOperations = 100;
	int nThreads = 10;
	int minValue = 0;
	int maxValue = 5;

	for (int i = 0; i < iterations; i ++) {
	    System.out.println(i + ": Iteration started!");
	    BST tree = new BST();
	    List<Thread> threads = new ArrayList<Thread>();
	    for (int t = 0; t < nThreads; t++) {
		if (t % 2 == 0)
		    threads.add( new Inserter(tree, nOperations, minValue, maxValue) );
		else
		    threads.add( new Deleter(tree, nOperations, minValue, maxValue) );
	    }

	    for (Thread t : threads)
		t.start();

	    try {
		for (Thread t : threads)
		    t.join();		
	    } catch (InterruptedException e) {
		System.out.println("Whatever..");
	    }
	    System.out.println(i+": Iteration completed!");
	    if (!tree.verify()) {
		System.out.println("Fuck");
		System.out.println(tree.prettyPrint());
		System.exit(1);
	    } else
		System.out.println(i+": Iteration verified!");
	}
    }    
}

class Inserter extends Thread {

    private BST tree;
    int nOps;
    int minValue,maxValue;

    public Inserter(BST tree,int nOps, int minValue, int maxValue) {
	this.tree = tree;
	this.nOps = nOps;
	this.minValue = minValue;
	this.maxValue = maxValue;
    }

    public void run() {
	for (int i = 0; i < nOps; i++) {
	    int el = ThreadLocalRandom.current().nextInt(minValue,maxValue);
	    System.out.println("Thread:"+this.getId()+" Insert(" + el + ") - " + tree.prettyPrint());
	    try {
		tree.insert(el);	    
	    } catch (NullPointerException e) {
		System.out.println("WTF");
		System.exit(1);
	    }
	}
    }
}

class Deleter extends Thread {

    private BST tree;
    int nOps;
    int minValue, maxValue;

    public Deleter(BST tree,int nOps, int minValue, int maxValue) {
	this.tree = tree;
	this.nOps = nOps;
	this.minValue = minValue;
	this.maxValue = maxValue;
    }

    public void run() {
	for (int i = 0; i < nOps; i++) {
	    int el = ThreadLocalRandom.current().nextInt(minValue,maxValue);
	    System.out.println("Thread:"+this.getId()+" Delete(" + el + ") - " + tree.prettyPrint());
	    try {
		tree.delete(el);
	    } catch (NullPointerException e) {
		System.out.println("WTF");
		System.exit(1);
	    }
	}
    }
}

