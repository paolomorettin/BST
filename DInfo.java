
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

public class DInfo extends Info {

    public Node gp;
    public AtomicStampedReference<Info> pupdate;

    public DInfo(Node gp, Node p, Node l, AtomicStampedReference<Info> pupdate) {
	super(p,l);
	this.gp = gp;
	this.pupdate = pupdate;
    }
}
    
    
