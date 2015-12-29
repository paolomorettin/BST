
//import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ThreadLocalRandom;

public class BST {

    AtomicReference<Node> root = null;
    //    AtomicInteger rootFlagMark;

    public BST() {
	root = new AtomicReference<Node>(null);
	//rootFlagMark = new AtomicInteger(Node.FREE);
    }

    // XXX synchronized? really?
    public synchronized String prettyPrint() {
	if (root.get() != null)
	    return root.get().prettyPrint();
	else
	    return "Empty tree";
    }

    public synchronized String prettyLeaves() {
	if (root.get() != null)
	    return root.get().prettyLeaves();
	else
	    return "Empty tree";
    }

    public boolean verify() {
	return root.get().verify(Integer.MIN_VALUE,Integer.MAX_VALUE);
    }

    public void insert(int key) {
	while (true) {
	    Node temp_root = root.get();
	    // CASE: no tree at all
	    if (temp_root == null) {
		Node newNode = new Node(key,null,null);
		if (root.compareAndSet(null,newNode))
		    return;
	    }
	    // CASE: just a single leaf
	    else if (temp_root.isLeaf()) {
		if (temp_root.getKey() < key) {
		    Node newNode = new Node(key,new Node(temp_root.getKey(),null,null),new Node(key, null, null));
		    //if (rootFlagMark.compareAndSet(Node.FREE,Node.FLAG)) {
			if (root.compareAndSet(temp_root,newNode)) {
			    //rootFlagMark.set(Node.FREE);
			    return;
			}
			//rootFlagMark.set(Node.FREE);
			//}
		} else if (temp_root.getKey() > key) {
		    Node newNode = new Node(temp_root.getKey(),new Node(key, null, null),new Node(temp_root.getKey(),null,null));
		    //if (rootFlagMark.compareAndSet(Node.FREE,Node.FLAG)) {
			if (root.compareAndSet(temp_root,newNode)) {
			    //rootFlagMark.set(Node.FREE);
			    return;
			}
			//rootFlagMark.set(Node.FREE);
			//}
		} else {
		    return;
		}
	    }
	    // CASE: non-trivial tree
	    else {
		// traverse the tree until a leaf is found
		Node n = root.get();
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
			if (p.getKey() < key) {
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
	if (root != null) {
	    if (root.isLeaf() && root.getKey() == key) {
		root = null;
	    } else {
		System.out.printf("NONTRIVIAL: ");
		Node g,p,n;
		g = p = n = root;
		while (!n.isLeaf()) {
		    g = p;
		    p = n;
		    if (n.getKey() > key)
			n = n.left.get();
		    else
			n = n.right.get();
		}
		System.out.printf("g: %d p: %d n: %d\n",g.getKey(),p.getKey(),n.getKey());
		if (n.getKey() == key) {
		    Node newBranch;
		    if (n.getKey() < p.getKey())
			newBranch = p.right.get();			    
		    else
			newBranch = p.left.get();
		    if (g == p) {
			root = newBranch;
		    } else {
			if (g.getKey() >= p.getKey())
			    g.setLeft(newBranch);
			else
			    g.setRight(newBranch);
		    }
		}
	    }
	}
    
    }

							  
    public static void main (String[] args) {
	int iterations = 10000;
	int nInsert = 100;
	for (int i = 0; i < iterations; i ++) {
	    BST tree = new BST();
	    Inserter t1 = new Inserter(tree,nInsert);
	    Inserter t2 = new Inserter(tree,nInsert);
	    Inserter t3 = new Inserter(tree,nInsert);
	    Inserter t4 = new Inserter(tree,nInsert);
	    t1.start();
	    t2.start();
	    t3.start();
	    t4.start();
	    try {
		t1.join();
		t2.join();
		t3.join();
		t4.join();
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
    int nInsert;

    public Inserter(BST tree,int nInsert) {
	this.tree = tree;
	this.nInsert = nInsert;
    }

    public void run() {
	for (int i = 0; i < nInsert; i++) {
	    int el = ThreadLocalRandom.current().nextInt(0,5);
	    tree.insert(el);
	}
    }
}

