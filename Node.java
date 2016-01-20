
import java.util.*;
import java.util.concurrent.atomic.*;

public class Node {

    // values for dummy nodes
    public static final int DUMMY1 = Integer.MAX_VALUE;
    public static final int DUMMY2 = Integer.MAX_VALUE - 1;

    // mark/flag values
    public static final int CLEAN = 0;
    public static final int IFLAG = 1;
    public static final int DFLAG = 2;
    public static final int MARK = 3;

    private int key;
    public AtomicStampedReference<Info> state;
    public AtomicReference<Node> left, right;
    private boolean isLeaf;

    public Node(int key, Node left, Node right) {
	// constructor for an internal node
	this.key = key;
	this.left = new AtomicReference<Node>(left);
	this.right = new AtomicReference<Node>(right);
	state = new AtomicStampedReference<Info>(null,CLEAN);
	isLeaf = false;
    }

    public Node(int key) {
	// constructor for a leaf
	this.key = key;
	this.left = null;
	this.right = null;
	state = null;
	isLeaf = true;
    }

    public boolean isLeaf() {
	return isLeaf;
    }

    public int getKey() {
	return key;
    }

    public boolean verify(int lower, int upper) {
	/* this method recursively verifies upper/lower bounds on the keys */
	if (key != Node.DUMMY1 && key != Node.DUMMY2) {
	    if (key < lower)
		return false;
	    if (key >= upper)
		return false;
	}

	if (!isLeaf) {
	    Node l = left.get();
	    Node r = right.get();
	    return l.verify(lower,key) && r.verify(key,upper);
	}
	return true;
    }
    public String prettyPrint() {
	/* this method returns a string representation of the current subtree
	   internal nodes are enclosed in round brackets, leaves are enclosed in square brackets
	   e.g. ( 5 [ 3 ] ( 7 [ 5 ] [ 7 ] ) )
	*/
	if (isLeaf)
	    return "[ " + key + " ]";
	else
	    return "( " + key + " " + left.get().prettyPrint() + " " + right.get().prettyPrint() + " )";
    }

    public TreeSet<Integer> getKeys() {
	/* this method returns the set of keys of the current subtree */
	TreeSet<Integer> result = new TreeSet<Integer>();
	if (isLeaf) {
	    if (key != Node.DUMMY1 && key != Node.DUMMY2)
		result.add(key);
	}
	else {
	    result.addAll(left.get().getKeys());
	    result.addAll(right.get().getKeys());
	}
	return result;
    }

    public String DOTFormat() {
	/* this method returns a string representing the current subtree in .DOT format 
	   (without the graph name and curly brackets)
	*/
	if (isLeaf)
	    return "l" + key + "\nl" + key + " [label="+ key + "]\n";
	else
	    return "i" + key + " -> " + left.get().DOTFormat() + "i" + key + " -> " + right.get().DOTFormat() + "i" + key + " [label="+ key + "]\n";
    }    
}
	

    
    
    
    
    
