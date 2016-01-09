
import java.util.*;
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

	Node p, newInternal, l, newSibling;
	Node newLeaf = new Node(key);
	IInfo op;
	Info pref;
	int pstamp;
	
	while (true) {
	    SearchResult res = search(key); // traverse the tree
	    l = res.l;
	    p = res.p;
	    pstamp = res.pstamp;
	    pref = res.pref;	    

	    int lKey = l.getKey();
	    if (lKey == key) // duplicate key, abort
		return false;

	    if (pstamp != Node.CLEAN) { // there's a pending operation on parent node: help and start over
		//help(p.state);
		help(pref,pstamp);
	    }
	    else {
		newSibling = new Node(lKey); // create the new internal node
		if (key < lKey)
		    newInternal = new Node(lKey,newLeaf,newSibling);
		else
		    newInternal = new Node(key,newSibling,newLeaf);

		op = new IInfo(p,l,newInternal); // new info record for the current insert

		if (p.state.compareAndSet(pref,op,pstamp,Node.IFLAG)) { // "iflag" CAS
		    helpInsert(op); // successful "iflag", complete the operation
		    return true;
		}
		else {
		    int[] stampHolder = new int[1];
		    Info refHolder = p.state.get(stampHolder);
		    help(refHolder,stampHolder[0]);
		}
	    }

	}	
    }
    public boolean delete(int key) {

	if (key == Node.DUMMY1 || key == Node.DUMMY2) // can't delete dummy key values
	    return false;

	Node gp, p, l;
	Info gpref, pref;
	int gpstamp, pstamp;
	DInfo op;

	while (true) {
	    System.out.println(Thread.currentThread().getId()+" Delete("+key+") cycle");
	    SearchResult res = search(key); // traverse the tree
	    gp = res.gp;
	    p = res.p;
	    l = res.l;
	    gpstamp = res.gpstamp;
	    pstamp = res.pstamp;
	    gpref = res.gpref;
	    pref = res.pref;

	    int lKey = l.getKey();
	    if (lKey != key) // the key is not present, abort
		return false;

	    if (gpstamp != Node.CLEAN) { // pending operation on grandparent, help
		System.out.println(Thread.currentThread().getId()+" Delete("+key+") -> help gp");
		help(gpref,gpstamp);
	    }
	    else if (pstamp != Node.CLEAN) { // pending operation on parent, help
		System.out.println(Thread.currentThread().getId()+" Delete("+key+") -> help p");
		help(pref,pstamp); // XXX
	    }
	    else {
		op = new DInfo(gp,p,l,pref,pstamp); // create a record with information on the current delete
		if (gp.state.compareAndSet(gpref,op,gpstamp,Node.DFLAG)) { // "dflag" CAS
		    System.out.println(Thread.currentThread().getId()+" Delete("+key+") -> helpDelete (after successful dflag)");
		    if (helpDelete(op)) // either complete the delete or "dunflag"
			return true;
		}
		else {
		    System.out.println(Thread.currentThread().getId()+" Delete("+key+") -> help (after unsuccessful dflag)");
		    //help(gp.state.getReference(),gp.state.getStamp());
		    int[] stampHolder = new int[1];
		    Info refHolder = gp.state.get(stampHolder);
		    help(refHolder,stampHolder[0]);
		}
	    }
	}	
    }

    public boolean find(int key) {
	return (search(key).l.getKey() == key);
    }

    private SearchResult search(int key) {
	SearchResult res = new SearchResult();
	res.l = root.get();
	while (!res.l.isLeaf()) {
	    res.gp = res.p;
	    res.p = res.l;
	    res.gpstamp = res.pstamp;
	    res.gpref = res.pref;

	    int[] aux = new int[1];
	    res.pref = res.p.state.get(aux);
	    res.pstamp = aux[0];
	    //res.pstamp = res.p.state.getStamp();
	    //res.pref = res.p.state.getReference();
	    if (key < res.l.getKey())
		res.l = res.p.left.get();
	    else
		res.l = res.p.right.get();
	}	

	return res;
    }

    private void help(Info ref,int stamp) { // generic helping routine
	int debug = -1;
	try {
	switch (stamp) {
	case (Node.IFLAG) : debug = 1; helpInsert((IInfo)ref); break;
	case (Node.MARK) : debug = 2; helpMarked((DInfo)ref); break;
	case (Node.DFLAG) : debug = 3; helpDelete((DInfo)ref); break;
	default: break;
	}
	} catch (ClassCastException e) { System.out.println("Eccolo! Caso "+debug + " " + ref); System.exit(1);}
    }

    /*
    private void help(AtomicStampedReference<Info> update) { // generic helping routine
	int debug = -1;
	try {
	switch (update.getStamp()) {
	case (Node.IFLAG) : debug = 1; helpInsert((IInfo)update.getReference()); break;
	case (Node.MARK) : debug = 2; helpMarked((DInfo)update.getReference()); break;
	case (Node.DFLAG) : debug = 3; helpDelete((DInfo)update.getReference()); break;
	default: break;
	}
	} catch (ClassCastException e) { System.out.println("Eccolo! Caso "+debug); System.exit(1);}
	}*/

    private void helpInsert(IInfo op) {
	assert (op != null);

	System.out.println(Thread.currentThread().getId()+" helpInsert -> CASChild");
	CASChild(op.p, op.l, op.newInternal); // "ichild" CAS
	op.p.state.compareAndSet(op,op,Node.IFLAG,Node.CLEAN); // "iunflag" CAS
    }

    private boolean helpDelete(DInfo op) {
	assert (op != null);

	System.out.println(Thread.currentThread().getId()+" helpDelete");
	/*
	int[] result = {0,0};
	op.p.state.get(result);
	*/
	if ((op.p.state.getReference() == op && op.p.state.getStamp() == Node.MARK) || op.p.state.compareAndSet(op.pref,op,op.pstamp,Node.MARK)) {
	    System.out.println(Thread.currentThread().getId()+" helpDelete -> helpMarked");
	    helpMarked(op);
	    return true;
	}
	else {
	    System.out.println(Thread.currentThread().getId()+" helpDelete -> help");
	    //help(op.p.state.getReference(),op.p.state.getStamp());
	    int[] stampHolder = new int[1];
	    Info refHolder = op.p.state.get(stampHolder);
	    help(refHolder,stampHolder[0]);
	    op.gp.state.compareAndSet(op,op,Node.DFLAG,Node.CLEAN); // unflag
	    return false;
	}
    }

    private void helpMarked(DInfo op) {
	assert (op != null);

	System.out.println(Thread.currentThread().getId()+" helpMarked");
	Node other;
	if (op.p.right.get() == op.l)
	    other = op.p.left.get();
	else
	    other = op.p.right.get();
	System.out.println(Thread.currentThread().getId()+" helpMarked -> CASChild");
	CASChild(op.gp,op.p,other);
	op.gp.state.compareAndSet(op,op,Node.DFLAG,Node.CLEAN);
	/*
	if (!op.gp.state.compareAndSet(op,op,Node.DFLAG,Node.CLEAN)) { // unflag
	    System.out.printf("DUNFLAG failed\n");
	    //System.exit(1);
	    
	    }*/
    }

    private void CASChild(Node parent, Node old, Node newNode) {
	assert (parent != null && newNode != null);

	System.out.println(Thread.currentThread().getId()+" CASChild");
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

    public TreeSet<Integer> getKeys() {
	return root.get().getKeys();
    }

    private class SearchResult { // ugly hack
	public Node l = null;
	public Node p = null;
	public Node gp = null;
	/*
	public AtomicStampedReference<Info> gpstate = null;
	public AtomicStampedReference<Info> pstate = null;
	*/

	public Info gpref,pref;
	public int gpstamp, pstamp;
    }

}

    
