package IndyLibraries;

public class CreateCredentialResult {

    public String credentialJson;
    public String revocId;
    public String revocRegDeltaJson;
    public CreateCredentialResult(String credentialJson, String revocId, String revocRegDeltaJson) {
        this.credentialJson=credentialJson;
        this.revocId=revocId;
        this.revocRegDeltaJson=revocRegDeltaJson;
    }
}
