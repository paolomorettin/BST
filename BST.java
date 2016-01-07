
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

public class BST {

    private AtomicReference<Node> root;
    
    public BST() {
	// Initialize the tree with dummy nodes.
	Node l_dummy = new Node(Node.DUMMY2);
	Node r_dummy = new Node(Node.DUMMY1);
	root = new AtomicReference<Node>( new Node(Node.DUMMY1,l_dummy,r_dummy) );
    }

    public boolean insert(int key) {

	if (key == Node.DUMMY1 || key == Node.DUMMY2) // can't insert dummy key values
	    return false;

	//Node p = null, newInternal = null, l = null, newSibling = null;
	Node p, newInternal, l, newSibling;
	Node newLeaf = new Node(key);
	IInfo op;
	AtomicStampedReference<Info> pupdate;
	
	while (true) {
	    SearchResult res = search(key);
	    l = res.l;
	    p = res.p;
	    pupdate = res.pupdate;	    

	    int lKey = l.getKey();
	    if (lKey == key) // duplicate key
		return false;
	    if (pupdate.getStamp() != Node.CLEAN) { // help another process
		help(pupdate);
	    }
	    else {
		newSibling = new Node(lKey); // create the new internal node
		if (key < lKey)
		    newInternal = new Node(lKey,newLeaf,newSibling);
		else
		    newInternal = new Node(key,newSibling,newLeaf);

		op = new IInfo(p,l,newInternal);		
		if (p.state.compareAndSet(pupdate.getReference(),op,pupdate.getStamp(),Node.IFLAG)) {
		    helpInsert(op);
		    return true;
		}
		else
		    help(p.state);
	    }

	}	
    }
    public boolean delete(int key) {

	if (key == Node.DUMMY1 || key == Node.DUMMY2) // can't delete dummy key values
	    return false;

	Node gp, p, l;
	AtomicStampedReference<Info> gpupdate, pupdate;
	DInfo op;

	while (true) {
	    SearchResult res = search(key);
	    gp = res.gp;
	    p = res.p;
	    l = res.l;
	    gpupdate = res.gpupdate;
	    pupdate = res.pupdate;

	    int lKey = l.getKey();
	    if (lKey != key) // the key is not present
		return false;

	    if (gpupdate.getStamp() != Node.CLEAN)
		help(gpupdate);
	    else if (pupdate.getStamp() != Node.CLEAN)
		help(pupdate);

	    // no help needed
	    else {
		op = new DInfo(gp,p,l,pupdate);
		if (gp.state.compareAndSet(gpupdate.getReference(),op,gpupdate.getStamp(),Node.DFLAG)) {
		    if (helpDelete(op))
			return true;
		}
		else
		    help(gp.state);
	    }
	}	
    }

    public boolean find(int key) {
	Node l = null, gp = null, p = null;
	AtomicStampedReference<Info> gpupdate = null, pupdate = null;
	//search(key,gp,p,l,gpupdate,pupdate);
	return (search(key).l.getKey() == key);
    }

    private SearchResult search(int key) {
	SearchResult res = new SearchResult();
	res.l = root.get();
	while (!res.l.isLeaf()) {
	    res.gp = res.p;
	    res.p = res.l;
	    res.gpupdate = res.pupdate;
	    res.pupdate = res.p.state;
	    if (key < res.l.getKey())
		res.l = res.p.left.get();
	    else
		res.l = res.p.right.get();
	}	
	return res;
    }

    private void help(AtomicStampedReference<Info> update) {
	switch (update.getStamp()) {
	case (Node.IFLAG) : helpInsert((IInfo)update.getReference()); break;
	case (Node.MARK) : helpMarked((DInfo)update.getReference()); break;
	case (Node.DFLAG) : helpDelete((DInfo)update.getReference()); break;
	}
    }

    private void helpInsert(IInfo op) {
	CASChild(op.p, op.l, op.newInternal);
	op.p.state.compareAndSet(op,op,Node.IFLAG,Node.CLEAN); // unflag
    }

    private boolean helpDelete(DInfo op) {
	int[] result = {0,0};
	op.p.state.get(result);
	if ((op.p.state.getReference() == op && op.p.state.getStamp() == Node.MARK) || op.p.state.compareAndSet(op.pupdate.getReference(),op,op.pupdate.getStamp(),Node.MARK)) {
	    helpMarked(op);
	    return true;
	}
	else {
	    help(op.p.state);
	    op.gp.state.compareAndSet(op,op,Node.DFLAG,Node.CLEAN); // unflag
	    return false;
	}
    }

    private void helpMarked(DInfo op) {
	Node other;
	if (op.p.right.get() == op.l)
	    other = op.p.left.get();
	else
	    other = op.p.right.get();
	CASChild(op.gp,op.p,other);
	op.gp.state.compareAndSet(op,op,Node.DFLAG,Node.CLEAN); // unflag
    }

    private void CASChild(Node parent, Node old, Node newNode) {
	if (newNode.getKey() < parent.getKey())
	    parent.left.compareAndSet(old,newNode);
	else
	    parent.right.compareAndSet(old,newNode);
    }

    // pretty printing and validation
    public boolean verify() {
	    return root.get().verify(Integer.MIN_VALUE,Integer.MAX_VALUE);
    }

    public synchronized String prettyPrint() {
	Node temp = root.get().left.get(); // do not print dummy nodes
	if (!temp.isLeaf())
	    return temp.left.get().prettyPrint();
	else
	    return "Empty tree";
    }

    private class SearchResult { // ugly hack
	public Node l = null;
	public Node p = null;
	public Node gp = null;
	public AtomicStampedReference<Info> gpupdate = null;
	public AtomicStampedReference<Info> pupdate = null;
    }

}

    
