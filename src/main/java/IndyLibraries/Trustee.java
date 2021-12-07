package IndyLibraries;

import org.hyperledger.indy.sdk.IndyConstants;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.ledger.Ledger;
import org.hyperledger.indy.sdk.pool.Pool;

import java.util.concurrent.ExecutionException;

import static org.hyperledger.indy.sdk.ledger.Ledger.signAndSubmitRequest;

public class Trustee extends Endorser{
    public Trustee (Pool poolConnection, String agentName, JSONUserCredentialStorage agentsFile) {
        super(poolConnection, agentName, agentsFile);
    }
    //assign an IndyLibraries.Endorser/Trust_Anchor role to the specified newEndorserDID,
    //creating a NymRequest,signign it and submitting it to the ledger
    public boolean assignStewardRole(DIDStructure newEndorserDid){
        String endorserMeaning;
        String nymRequest,nymResponseJson;
        try {
            nymRequest = Ledger.buildNymRequest(mainDID.didName,
                    newEndorserDid.didName, newEndorserDid.didVerKey, null, IndyConstants.ROLE_STEWARD).get();
            System.out.println("request to assign steward role : \n"+nymRequest);

            nymResponseJson = signAndSubmitRequest(poolConnection, this.mainWallet, this.mainDID.didName, nymRequest).get();
            System.out.println(nymResponseJson);
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
    //can only add attribute to his own nym
    public boolean addAttributeToNYM(String[] rawID, String[] rawValues){
        String attribReq;
        String attribResponse;
        String raw = IndyJsonStringBuilder.buildRawAttrJSON(rawID,rawValues);
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


}
