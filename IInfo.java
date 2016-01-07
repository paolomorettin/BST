
public class IInfo extends Info {

    public Node newInternal;

    public IInfo(Node p, Node l, Node newInternal) {
	super(p,l);
	this.newInternal = newInternal;
    }

}
