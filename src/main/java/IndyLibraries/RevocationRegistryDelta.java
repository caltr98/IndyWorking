package IndyLibraries;

public class RevocationRegistryDelta {
    String id;
    String objectJson;
    long timestamp;
    public RevocationRegistryDelta(String id, String objectJson, long timestamp) {
        this.id=id;
        this.objectJson=objectJson;
        this.timestamp=timestamp;
    }
}
