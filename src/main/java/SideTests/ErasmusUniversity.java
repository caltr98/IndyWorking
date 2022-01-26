package SideTests;

import IndyLibraries.*;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.pool.Pool;
import org.json.JSONObject;

import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

import static SideTests.sideTestIndy.getUnixTimeStamp;
import static java.lang.Thread.sleep;

public class ErasmusUniversity {
    public static void main(String[] args) {
        {

            String poolName="INDYSCANPOOL";
            String stewardSeed = "000000000000000000000000Steward1";
            String trusteeSeed = "000000000000000000000000Trustee1";
            Pool pool=null;
            File agentsFile=new File("./agentsFile.json");
            JSONUserCredentialStorage jsonStoredCred=null;
            try {
                jsonStoredCred= new JSONUserCredentialStorage(agentsFile);
            } catch (NoSuchPaddingException | NoSuchAlgorithmException | IOException e) {
                e.printStackTrace();
            }
            try {
                Pool.setProtocolVersion(2).get();

                //1. Create and Open Pool
                //poolName = IndyLibraries.PoolUtils.createPoolLedgerConfig();

                //Pool.createPoolLedgerConfig(poo   PlName,"{}").get();
                pool = Pool.openPoolLedger(poolName, "{}").get();
            } catch (InterruptedException | ExecutionException | IndyException e) {
                e.printStackTrace();
            }
            try {
                jsonStoredCred= new JSONUserCredentialStorage(agentsFile);
            } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Setup Steward");
            StewardAgent Ministry = new StewardAgent(pool,"Ministry",jsonStoredCred);
            Ministry.CreateWallet("stewardwallet","pass");
            Ministry.OpenWallet("stewardwallet","pass");
            Ministry.createDID(stewardSeed);
            System.out.println("Steward DID: "+ Ministry.getMainDID().didName+" "+
                    "Steward VerKey: "+Ministry.getMainDID().didVerKey);
            Endorser University = new Endorser(pool,"UNIPI",jsonStoredCred);
            University.CreateWallet("endorserwallet","abcd");
            University.OpenWallet("endorserwallet","abcd");
            University.createDID();

            boolean feedback=Ministry.assignEndorserRole(University.mainDID,true);

            System.out.println("\nUniversity DID: "+ University.getMainDID().didName+" "+
                    "University VerKey:"+University.getMainDID().didVerKey);

            String[] attributesForSchema={"name","mat"};

            SchemaStructure StudentIdentitySchema=University.
                    publishschema("StudentIdentity","3.0",attributesForSchema);

            System.out.println("Schema credenziali di ID: "+ StudentIdentitySchema.schemaId+": \n"+new JSONObject(StudentIdentitySchema.schemaJson).toString(4) + "\n");

            CredDefStructure credDefStudIdent=
                    University.IssuerCreateStoreAndPublishCredDef("TAG2",true,
                            StudentIdentitySchema.schemaId);

            Agent Student = new Agent(pool,"Alice",jsonStoredCred);

            Student.CreateWallet("4-AgentWallet411","passWORD");
            Student.OpenWallet("4-AgentWallet411","passWORD");

            Student.createDID();
            Student.createMasterSecret("0e6a6869-f288-491e-977c-d77ea1c348f6");

            System.out.println("\nAlice DID: "+ Student.getMainDID().didName+" "+
                    "Alice VerKey"+Student.getMainDID().didVerKey);

            CredOfferStructure credOfferToStudent=University.returnCredentialOffer(credDefStudIdent.credDefId);

            System.out.println("credential offer: \n"+new JSONObject(credOfferToStudent.credOffer).toString(4)+"\n");
            CredRequestStructure credRequestStructure=Student.returnproverCreateCredentialRequest(credOfferToStudent);
            CreateCredentialResult credential=University.returnIssuerCreateCredentialNonRevocable(attributesForSchema,new String[]{"Alice","00010"},
                    credOfferToStudent.credOffer, credRequestStructure.credReqJson
            );


            Student.storeCredentialInWallet(null,credOfferToStudent.credDef.credDefId,credRequestStructure.credReqMetadataJson,
                    credential.credentialJson,credOfferToStudent.credDef.credDefJson,null);


            try {//wait for changes to be written in the Ledger
                sleep((3*1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Endorser ErasmusUniversity = new Endorser(pool,"UniversityofGraz",jsonStoredCred);
            ErasmusUniversity.CreateWallet("UniversityofGrazWallet","UniversityofGrazPass");
            ErasmusUniversity.OpenWallet("UniversityofGrazWallet","UniversityofGrazPass");
            ErasmusUniversity.createDID();
            long timestampProofReq= getUnixTimeStamp();
            feedback=Ministry.assignEndorserRole(ErasmusUniversity.mainDID ,true);
            try {
                sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            JSONObject attributeGenerated=ErasmusUniversity.generateAttrInfoForProofRequest(attributesForSchema[0],null,
                    null,
                    University.mainDID.didName,
                    null,
                    null,
                    University.mainDID.didName,
                    credDefStudIdent.credDefId,
                    null, null, null,null,null);

            String proofReqbody=ErasmusUniversity.returnVerifierGenerateProofRequest("reproof1","1.0","1.0",
                    new JSONObject[]{attributeGenerated}
                    ,null,null,null);


            ProofAttributesFetched proofStruct=Student.returnProverSearchAttrForProof(proofReqbody,null);
            ProofCreationExtra proofCreation= Student.proverCreateProof(proofStruct,proofReqbody,null,null, -1);


            System.out.println("proof is valid? "+
                    ErasmusUniversity.returnVerifierVerifyProofNOREVOCATION(proofReqbody,proofCreation.proofJson,proofCreation.schemas,proofCreation.credentialDefs));
        }
    }

}
