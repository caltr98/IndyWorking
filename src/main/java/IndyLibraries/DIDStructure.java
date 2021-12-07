package IndyLibraries;

public class DIDStructure {

    public String didName;
    public String didVerKey;
    public String didEndpoint;
    public DIDStructure (String didName,String didVerKey){
        this.didName=didName;
        this.didVerKey=didVerKey;
    }
    public DIDStructure (String didName,String didVerKey,String didEndpoint){
        this.didName=didName;
        this.didVerKey=didVerKey;
        this.didEndpoint = didEndpoint;
    }

    public void setVerKey(String didVerKey){
        this.didVerKey=didVerKey;
    }
    public void setDID(String didVerKey){
        this.didVerKey=didVerKey;
    }
}
