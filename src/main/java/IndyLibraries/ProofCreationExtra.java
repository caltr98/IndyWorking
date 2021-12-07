package IndyLibraries;

public class ProofCreationExtra {
    public String proofRequestJson;
    public String proofJson;
    public String schemas;
    public String credentialDefs;

    public ProofCreationExtra(String proofRequestJson, String proofJson, String schemas, String credentialDefs) {
        this.proofRequestJson = proofRequestJson;
        this.proofJson = proofJson;
        this.schemas = schemas;
        this.credentialDefs = credentialDefs;
    }
}
