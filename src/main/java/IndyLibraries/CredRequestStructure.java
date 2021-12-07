package IndyLibraries;

public class CredRequestStructure {
    public String credReqJson;
    public String credReqMetadataJson ;
    public CredRequestStructure(String credReqJson,String credReqMetadataJson){
        this.credReqJson= credReqJson;
        this.credReqMetadataJson= credReqMetadataJson;
    }
}
