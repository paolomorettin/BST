
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class Node {
    public static final int FREE = 0;
    public static final int FLAG = 1;
    public static final int MARK = 2;

    private final int key;
    public AtomicReference<Node> left, right;
    public AtomicInteger flagMark;

    public Node(int key, Node left, Node right) {
	this.key = key;
	this.left = new AtomicReference<Node>(left);
	this.right = new AtomicReference<Node>(right);
	this.flagMark = new AtomicInteger(Node.FREE);
    }

    public String prettyPrint() {
	if (left.get() == null && right.get() == null)
	    return "[ " + key + " ]";
	else
	    return "( " + key + " {" + flagMark.get() + "} " + left.get().prettyPrint() + " " + right.get().prettyPrint() + " )";
    }

    public String prettyLeaves() {
	if (left.get() == null && right.get() == null)
	    return " " + key;
	else
	    return left.get().prettyLeaves() + right.get().prettyLeaves();
    }

    public boolean verify(int lower, int upper) {
	// verify bounds on key
	if (key < lower)
	    return false;
	if (key >= upper)
	    return false;

	Node l = left.get();
	Node r = right.get();

	if ((l == null && r != null) || (l != null && r == null))
	    return false;

	// if it's not a leaf, verify branches
	if (l != null && right != null)
	    return l.verify(lower,key) && r.verify(key,upper);

	// verify structure (both pointers to null, or none of them)
	else
	    return true; //!((l == null && right != null) || (l != null && right == null));
    }

    public int getKey () { return key; }
    public boolean isLeaf () { return (left.get() == null && right.get() == null); }
}
    
