
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

public class DInfo extends Info {

    public Node gp;
    public Info pref;
    public int pstamp;

    public DInfo(Node gp, Node p, Node l, Info pref, int pstamp) {
	super(p,l);
	this.gp = gp;
	this.pstamp = pstamp;
	this.pref = pref;
    }
}
    
    
