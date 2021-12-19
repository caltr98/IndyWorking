package IndyLibraries;

import org.hyperledger.indy.sdk.did.DidResults;

public class AskDID2DIDresult {
    DidResults.CreateAndStoreMyDidResult agentDIDResult;
    byte[] toSend;
    public AskDID2DIDresult(DidResults.CreateAndStoreMyDidResult agentDIDResult, byte[] toSend) {
        this.agentDIDResult=agentDIDResult;
        this.toSend=toSend;
    }
}
