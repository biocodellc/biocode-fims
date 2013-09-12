package digester;

/**
 * Add the core FIMS object
 */
public class Fims {
    private Metadata metadata;

    public void addMetadata(Metadata m) {
        metadata = m;
    }

    public void print() {
        metadata.print();
    }
}
