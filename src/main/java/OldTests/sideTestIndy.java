package OldTests;

import IndyLibraries.*;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.pool.Pool;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.concurrent.ExecutionException;

import static java.lang.Thread.sleep;

public class sideTestIndy {
    //This simulates the case of a University giving Student a credentatial to prove he is enrolled to said University
    //to an external service,the external service verify the proof,
    // the credential can be revoked by the university (eg:student has graduated) when the credential is revoked and any
    //proof request that require the credential to be valid in the current moment will find  proof from the credential non-valid
    public static void main(String[] args) {

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
        StewardAgent Ministero = new StewardAgent(pool,"ministero",jsonStoredCred);
        Ministero.CreateWallet("stewardwallet","pass");
        Ministero.OpenWallet("stewardwallet","pass");
        Ministero.createDID(stewardSeed);
        System.out.println("Steward DID: "+ Ministero.getMainDID().didName+" "+
                "Steward VerKey: "+Ministero.getMainDID().didVerKey);
        Endorser University = new Endorser(pool,"UNIPI",jsonStoredCred);
        University.CreateWallet("endorserwallet","abcd");
        University.OpenWallet("endorserwallet","abcd");
        University.createDID();

        boolean feedback=Ministero.assignEndorserRole(University.mainDID,true);

        System.out.println("\nUniversity DID: "+ University.getMainDID().didName+" "+
                "University VerKey:"+University.getMainDID().didVerKey);

        String[] attributesForSchema={"name"};

        SchemaStructure StudentIdentitySchema=University.
                publishschema("StudentIdentity","1.0",attributesForSchema);

        System.out.println("Schema credenziali di ID: "+ StudentIdentitySchema.schemaId+": \n"+new JSONObject(StudentIdentitySchema.schemaJson).toString(4) + "\n");

        CredDefStructure credDefStudIdent=
                University.IssuerCreateStoreAndPublishCredDef("TAG2",true,
                        StudentIdentitySchema.schemaId);
        //creating revocation registry for credentials issued by the university
        RevocationRegistryObject revreg=University.createRevocationRegistry(null,"Hi",credDefStudIdent.credDefId,"ISSUE_ON_DEMAND",
                "5"
                );
        String revocationRegistry=University.publishRevocationRegistryDefinition(revreg);
        String revRegFirsTEntry = University.publishRevocationRegistryEntry(revreg,revreg.revRegEntryJson, getUnixTimeStamp());

        Agent Studente = new Agent(pool,"Alice",jsonStoredCred);
        //necessario avere un wallet nuovo ad ogni esecuzione al momento

        Studente.CreateWallet("4-AgentWallet411","passWORD");
        Studente.OpenWallet("4-AgentWallet411","passWORD");

        Studente.createDID();
        Studente.createMasterSecret("0e6a6869-f288-491e-977c-d77ea1c348f6");

        System.out.println("\nAlice DID: "+ Studente.getMainDID().didName+" "+
                "Alice VerKey"+Studente.getMainDID().didVerKey);

        CredOfferStructure credOfferToStudent=University.returnCredentialOffer(credDefStudIdent.credDefId);

        System.out.println("credential offer: \n"+new JSONObject(credOfferToStudent.credOffer).toString(4)+"\n");
        CredRequestStructure credRequestStructure=Studente.returnproverCreateCredentialRequest(credOfferToStudent);
        System.out.println("credential request: \n"+new JSONObject(credRequestStructure.credReqJson ).toString(4)+"\n");
        System.out.println("credential request METADATA: \n"+new JSONObject(credRequestStructure.credReqMetadataJson ).toString(4)+"\n");

        CreateCredentialResult credential=University.returnIssuerCreateCredential(attributesForSchema,new String[]{"Alice"},
                credOfferToStudent.credOffer, credRequestStructure.credReqJson,revreg
        );

        University.publishRevocationRegistryEntry(revreg,credential.revocRegDeltaJson, getUnixTimeStamp());

        String credentialStore;
        //issuer must provide to prover the revocation registry associated with the credential so that prover
        //can use it during proof creation
        credentialStore=Studente.storeCredentialInWallet(null,credOfferToStudent.credDef.credDefId,credRequestStructure.credReqMetadataJson,
                credential.credentialJson,credOfferToStudent.credDef.credDefJson,revreg.revRegId);


        try {//wait for changes to be written in the Ledger
            sleep((3*1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String getDeltaForProof2=University.getDeltaForProof(revreg, getUnixTimeStamp());
        System.out.println("DELTA AFTER REVOCATION! \n "+getDeltaForProof2);
        String revoRegAccum2=University.getRevocRegReqGetAccum(revreg, getUnixTimeStamp());
        System.out.println("REVOC ACCUM AFTER REVOCATION! \n "+revoRegAccum2);

        Endorser Spotify = new Endorser(pool,"Spotify",jsonStoredCred);
        Spotify.CreateWallet("SpotifyWallet","SpassWORD");
        Spotify.OpenWallet("SpotifyWallet","SpassWORD");
        Spotify.createDID();
        long timestampProofReq= getUnixTimeStamp();
        feedback=Ministero.assignEndorserRole(Spotify.mainDID ,true);
        try {
            sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        JSONObject attributeGenerated=Spotify.generateAttrInfoForProofRequest(attributesForSchema[0],null,
                null,
                University.mainDID.didName,
                null,
                null,
                University.mainDID.didName,
                credDefStudIdent.credDefId,
                null, null, revreg.revRegId,timestampProofReq,timestampProofReq);

        System.out.println("generated attreinfo "+attributeGenerated.toString(4));
        try {
            sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String proofReqbody=Spotify.returnVerifierGenerateProofRequest("reproof1","1.0","1.0",
                new JSONObject[]{attributeGenerated}
                ,null,timestampProofReq,timestampProofReq);

        try {
            sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        timestampProofReq = getUnixTimeStamp();//new timestamp for proof req

        ProofAttributesFetched proofStruct=Studente.returnProverSearchAttrForProof(proofReqbody,null);
        ProofCreationExtra proffCreation= Studente.proverCreateProof(proofStruct,proofReqbody,null,timestampProofReq, revreg.blobStorageReaderHandle);
        JSONArray proofArray = new JSONObject(proffCreation.proofJson).getJSONArray("identifiers");
        JSONObject partofproof = proofArray.getJSONObject(0);

        String proof = new JSONObject(proffCreation.proofJson).put("identifiers",proofArray).toString(4);
        String deltaForProof = Studente.getDeltaForProof(revreg, getUnixTimeStamp());
        String revocRegs = new JSONObject().put(revreg.revRegId, new JSONObject().put(Long.toString(timestampProofReq), new JSONObject(deltaForProof))).toString();
        String revocRegDefs=new JSONObject().put(revreg.revRegId, new JSONObject(Studente.getRevocationDefinition(revreg.revRegId))).toString();

        System.out.println("proof is right BEFORE revocation? "+
                Spotify.returnVerifierVerifyProof(proofReqbody,proof,proffCreation.schemas,proffCreation.credentialDefs
                        ,revocRegDefs,revocRegs));

        revreg.revRegEntryJson = University.IssuerRevokeCredentialAndPublish(revreg,credential.revocId);
        getDeltaForProof2=University.getDeltaForProof(revreg, getUnixTimeStamp());
        System.out.println("DELTA AFTER REVOCATION! \n "+getDeltaForProof2);
        revoRegAccum2=University.getRevocRegReqGetAccum(revreg, getUnixTimeStamp());
        System.out.println("REVOC ACCUM AFTER REVOCATION! \n "+revoRegAccum2);


         proofStruct=Studente.returnProverSearchAttrForProof(proofReqbody,null);
         proffCreation= Studente.proverCreateProof(proofStruct,proofReqbody,null,timestampProofReq, revreg.blobStorageReaderHandle);
         proofArray = new JSONObject(proffCreation.proofJson).getJSONArray("identifiers");
         partofproof = proofArray.getJSONObject(0);

         proof = new JSONObject(proffCreation.proofJson).put("identifiers",proofArray).toString(4);
         deltaForProof = Studente.getDeltaForProof(revreg, getUnixTimeStamp());
         revocRegs = new JSONObject().put(revreg.revRegId, new JSONObject().put(Long.toString(timestampProofReq), new JSONObject(deltaForProof))).toString();
         revocRegDefs=new JSONObject().put(revreg.revRegId, new JSONObject(Studente.getRevocationDefinition(revreg.revRegId))).toString();

        System.out.println(credDefStudIdent);
        System.out.println("printing all information about proof verify :"+
                "\n createdProof.request 1: "+proofReqbody+"\n createdProof proof 2 :" + proof
                +"\n createdProof proof 3:" + proffCreation.schemas+
                "+\n created proof 4:"+ proffCreation.credentialDefs+
                "\n revocregdegs 5: "+ revocRegDefs +
                "\n revocReg 6:"+ revocRegs);

        //University.IssuerRevokeCredentialAndPublish(revreg,credential.revocId);

        try {
            sleep((3*1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("proof is right after revocation? "+
                Spotify.returnVerifierVerifyProof(proofReqbody,proof,proffCreation.schemas,proffCreation.credentialDefs
                        ,revocRegDefs,revocRegs));
        try {
            jsonStoredCred.makeBackup();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public static long getUnixTimeStamp() {
        return Instant.now().getEpochSecond();
    }

}
