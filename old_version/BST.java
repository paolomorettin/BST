
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ThreadLocalRandom;

public class BST {

    AtomicReference<Node> root = null;
    AtomicInteger rootFlag;

    public BST() {
	root = new AtomicReference<Node>(null);
	rootFlag = new AtomicInteger(Node.FREE);
    }

    public synchronized String prettyPrint() {
	Node temp_root = root.get();
	if (temp_root != null)
	    return temp_root.prettyPrint();
	else
	    return "Empty tree";
    }

    public synchronized String prettyLeaves() {
	Node temp_root = root.get();
	if (temp_root != null)
	    return temp_root.prettyLeaves();
	else
	    return "Empty tree";
    }

    public boolean verify() {
	Node temp_root = root.get();
	if (temp_root != null) 
	    return temp_root.verify(Integer.MIN_VALUE,Integer.MAX_VALUE);
	return true;
    }

    public void insert(int key) {
	System.out.println("Thread:"+Thread.currentThread().getId()+" Insert(" + key + ") - " + this.prettyPrint());
	while (true) {
	    
	    Node temp_root = root.get();
	    // CASE: no tree at all
	    if (temp_root == null) {
		if (rootFlag.compareAndSet(Node.FREE,Node.FLAG)) {
		    Node newNode = new Node(key,null,null);
		    if (root.compareAndSet(null,newNode)) {
			rootFlag.set(Node.FREE);
			return;
		    }
		    rootFlag.set(Node.FREE);
		}

	    }
	    // CASE: just a single leaf
	    else if (temp_root.isLeaf()) {
		Node newNode = null;
		if (temp_root.getKey() < key) 
		    newNode = new Node(key,new Node(temp_root.getKey(),null,null),new Node(key, null, null));
		else if (temp_root.getKey() > key) 
		    newNode = new Node(temp_root.getKey(),new Node(key, null, null),new Node(temp_root.getKey(),null,null));
		else
		    return;
		
		if (rootFlag.compareAndSet(Node.FREE,Node.FLAG)) {
		    if (root.compareAndSet(temp_root,newNode)) {
			rootFlag.set(Node.FREE);
			return;
		    }
		    rootFlag.set(Node.FREE);
		}
	    }
	    // CASE: non-trivial tree
	    else {
		// traverse the tree until a leaf is found
		Node n = temp_root;
		Node p = null;
		while (!n.isLeaf()) {
		    p = n;
		    if (key < n.getKey())
			n = n.left.get();
		    else
			n = n.right.get();
		}
		Node newNode = null;
		if (n.getKey() < key)
		    newNode = new Node(key,new Node(n.getKey(),null,null),new Node(key,null,null));
		else if (n.getKey() > key)
		    newNode = new Node(n.getKey(),new Node(key,null,null),new Node(n.getKey(),null,null));
		else
		    return;

		if (newNode != null) {
		    if (p.flagMark.compareAndSet(Node.FREE,Node.FLAG)) {
			if (p.getKey() <= n.getKey()) {
			    if (p.right.compareAndSet(n,newNode)) {
				p.flagMark.set(Node.FREE);
				return;
			    }
			}
			else {
			    if (p.left.compareAndSet(n,newNode)) {
				p.flagMark.set(Node.FREE);
				return;
			    }
			}
			p.flagMark.set(Node.FREE);
		    } 
		}
	    }
	}
    }

    public void delete(int key) {
	//System.out.println("Thread:"+Thread.currentThread().getId()+" Delete(" + key + ") - " + this.prettyPrint());
	while (true) {	    
	    Node temp_root = root.get();
	    if (temp_root == null)
		return;

	    // traverse the tree
	    Node g,p,n;
	    g = p = n = temp_root;
	    while (!n.isLeaf()) {
		g = p;
		p = n;
		if (n.getKey() > key)
		    n = n.left.get();
		else
		    n = n.right.get();
	    }

	    // key isn't there
	    if (n.getKey() != key)
		return;

	    // CASE: n is the root
	    if (n == p) {
		if (rootFlag.compareAndSet(Node.FREE,Node.FLAG)) {
		    if (p.flagMark.compareAndSet(Node.FREE,Node.MARK)) { // is this necessary? yes.
			if (root.compareAndSet(temp_root,null)) {
			    rootFlag.set(Node.FREE);
			    return;
			}
		    }
		    rootFlag.set(Node.FREE);
		}
	    }
	    else {
		Node newBranch;
		if (n.getKey() >= p.getKey())
		    newBranch = p.left.get();
		else 
		    newBranch = p.right.get();

		// CASE: n is a root child
		if (p == g) {
		    if (rootFlag.compareAndSet(Node.FREE,Node.FLAG)) {
			if (p.flagMark.compareAndSet(Node.FREE,Node.MARK)) {
			    if (root.compareAndSet(p,newBranch)) {
				rootFlag.set(Node.FREE);
				return;
			    }
			}
			rootFlag.set(Node.FREE);
		    }
		}
		// CASE: nontrivial case
		else {
		    if (g.flagMark.compareAndSet(Node.FREE,Node.FLAG)) {
			if (p.flagMark.compareAndSet(Node.FREE,Node.MARK)) {
			    if (p.getKey() < g.getKey()) {
				if (g.left.compareAndSet(p,newBranch)) {
				    g.flagMark.set(Node.FREE);
				    return;
				}
			    }
			    else {
				if (g.right.compareAndSet(p,newBranch)) {
				    g.flagMark.set(Node.FREE);
				    return;
				}
			    }
			}
			g.flagMark.set(Node.FREE);
		    }
		}    		
	    }
	}
    }	

							  
    public static void main (String[] args) {
	int iterations = 10000;
	int nOperations = 100;
	int nThreads = 10;
	int minValue = 0;
	int maxValue = 3;

	for (int i = 0; i < iterations; i ++) {
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
		System.out.println(tree.prettyLeaves());
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
	    //System.out.println("Thread:"+this.getId()+" Insert(" + el + ") - " + tree.prettyPrint());
	    try {
		tree.insert(el);	    
	    } catch (NullPointerException e) {
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
	    //System.out.println("Thread:"+this.getId()+" Delete(" + el + ") - " + tree.prettyPrint());
	    try {
		tree.delete(el);
	    } catch (NullPointerException e) {
		System.exit(1);
	    }
	}
    }
}

