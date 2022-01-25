package IndyLibraries;

import netscape.javascript.JSObject;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.ledger.Ledger;
import org.hyperledger.indy.sdk.pool.Pool;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

import static org.hyperledger.indy.sdk.ledger.Ledger.*;

public class StewardAgent extends Endorser {
    private final static String TRUST_ANCHOR="TRUST_ANCHOR";

    public StewardAgent(Pool poolConnection, String agentName, JSONUserCredentialStorage agentsFile) {
        super(poolConnection, agentName,agentsFile);
    }
    //assign an Indy Endorser/Trust_Anchor role to the specified newEndorserDID,
    //Agent here is creating a NymRequest,signing it and submitting it to the ledger,
    //a NymRequest will publish the newEndorser DID and VerKey on the Domain Ledger.
    public boolean assignEndorserRole(DIDStructure newEndorserDid,boolean isTrustAnchor){
        String endorserMeaning;
        String nymRequest;
        String nymResponseJson;
        if (isTrustAnchor)
            endorserMeaning=TRUST_ANCHOR;
        else endorserMeaning="101";
        try {
            nymRequest = Ledger.buildNymRequest(mainDID.didName,
                    newEndorserDid.didName, newEndorserDid.didVerKey, null, endorserMeaning).get();
            System.out.println(new JSONObject(nymRequest).toString(4));
            nymResponseJson = signAndSubmitRequest(poolConnection, this.mainWallet, this.mainDID.didName, nymRequest).get();
            //System.out.println(nymResponseJson);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        } catch (IndyException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    //NOTE:can only add attribute to his own nym
    public boolean addENdpointToNYM(String endpointName,String endpointAddres){
        String attribReq;
        String attribResponse;
        String raw = IndyJsonStringBuilder.endpointJson(endpointName,endpointAddres);
        try {
            attribReq = Ledger.buildAttribRequest(mainDID.didName,mainDID.didName
                    , null, raw,null).get();
            attribResponse = signAndSubmitRequest(poolConnection, this.mainWallet, this.mainDID.didName, attribReq).get();
            System.out.println("addAttribute Response : "+ attribResponse);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        } catch (IndyException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String GetvalidatorInfo(){
        try {
            String validatorInfoReq=buildGetValidatorInfoRequest(this.mainDID.didName).get();
            //validator info request must can be sent to specific node
            String SignedvalidatorInfoReq=signRequest(this.mainWallet,this.mainDID.didName,validatorInfoReq).get();
            //submitAction(Pool pool,String requestJson,String nodes,int timeout)
            String requestResult=submitAction(this.poolConnection,SignedvalidatorInfoReq,null,-1).get();
            System.out.println(requestResult);
            JSONObject result = new JSONObject(requestResult);
            System.out.println("node 1 info" +new JSONObject(result.getString("Node1")).toString(4));
            System.out.println("node 2 info" +new JSONObject(result.getString("Node2")).toString(4));
            System.out.println("node 3 info" +new JSONObject(result.getString("Node3")).toString(4));
            System.out.println("node 4 info" +new JSONObject(result.getString
                    ("Node4")).toString(4));

            return new JSONObject(requestResult).toString(4);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        return null;

    }

}
