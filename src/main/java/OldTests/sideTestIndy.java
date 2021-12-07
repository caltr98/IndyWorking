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
import java.util.concurrent.ExecutionException;

import static java.lang.Thread.sleep;

public class sideTestIndy {
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
        System.out.println("Recupero credenziali Steward");
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

        RevocationRegistryObject revreg=University.createRevocationRegistry(null,"Hi",credDefStudIdent.credDefId,"ISSUE_ON_DEMAND",
                "5"
                );
        String revocationRegistry=University.publishRevocationRegistryDefinition(revreg);
        String revRegFirsTEntry = University.publishRevocationRegistryEntry(revreg,revreg.revRegEntryJson, demo2.getUnixTimeStamp());

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
        CreateCredentialResult credential=University.returnIssuerCreateCredential(attributesForSchema,new String[]{"Alice"},
                credOfferToStudent.credOffer, credRequestStructure.credReqJson,revreg
        );

        University.publishRevocationRegistryEntry(revreg,credential.revocRegDeltaJson, demo2.getUnixTimeStamp());

        String credentialStore;
        //IMPORTANTE L'ISSUER DEVE PASSARE AL PROVER ANCHE IL REVOCATION REGISTRY CORRISPONDENTE ALLA PROOF
        credentialStore=Studente.storeCredentialInWallet(null,credOfferToStudent.credDef.credDefId,credRequestStructure.credReqMetadataJson,
                credential.credentialJson,credOfferToStudent.credDef.credDefJson,revreg.revRegId);

        String getDeltaForProof=University.getDeltaForProof(revreg, demo2.getUnixTimeStamp());
        System.out.println("DELTA BEFORE REVOCATION! \n "+getDeltaForProof);
        String revoRegAccum1=University.getRevocRegReqGetAccum(revreg, demo2.getUnixTimeStamp());
        System.out.println("REVOC ACCUM BEFORE REVOCATION! \n "+revoRegAccum1);

        revreg.revRegEntryJson = University.IssuerRevokeCredentialAndPublish(revreg,credential.revocId);
        try {
            sleep((3*1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String getDeltaForProof2=University.getDeltaForProof(revreg, demo2.getUnixTimeStamp());
        System.out.println("DELTA AFTER REVOCATION! \n "+getDeltaForProof2);
        String revoRegAccum2=University.getRevocRegReqGetAccum(revreg, demo2.getUnixTimeStamp());
        System.out.println("REVOC ACCUM AFTER REVOCATION! \n "+revoRegAccum2);

        Endorser Spotify = new Endorser(pool,"Spotify",jsonStoredCred);
        Spotify.CreateWallet("SpotifyWallet","SpassWORD");
        Spotify.OpenWallet("SpotifyWallet","SpassWORD");
        Spotify.createDID();
        long timestampProofReq= demo2.getUnixTimeStamp();
        feedback=Ministero.assignEndorserRole(Spotify.mainDID ,true);
        try {
            sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

         getDeltaForProof=University.getDeltaForProof(revreg,timestampProofReq);
        System.out.println("DELTA AFTER PROOF \n "+getDeltaForProof);

        String revState = University.createRevocationState(revreg,getDeltaForProof,timestampProofReq,credential.revocId);

        JSONObject revStates = new JSONObject();
        revStates.put(revreg.revRegId, new JSONObject().put(Long.toString(timestampProofReq), new JSONObject(revState)));


        JSONObject attributeGenerated=Spotify.generateAttrInfoForProofRequest(attributesForSchema[0],null,
                null,
                University.mainDID.didName,
                null,
                null,
                University.mainDID.didName,
                credDefStudIdent.credDefId,
                revreg.revRegId,timestampProofReq,timestampProofReq);

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
        timestampProofReq = demo2.getUnixTimeStamp();//new timestamp for proof req


        ProofAttributesFetched proofStruct=Studente.returnProverSearchAttrForProof(proofReqbody);
        System.out.println("sizeo of credrevIDS "+proofStruct.AttrCREDENTIALRevRegIDs.size());
        ProofCreationExtra proffCreation= Studente.proverCreateProof(proofStruct,proofReqbody,null,
                new String[]{"true"},timestampProofReq, revreg.blobStorageReaderHandle);
        JSONArray proofArray = new JSONObject(proffCreation.proofJson).getJSONArray("identifiers");
        JSONObject partofproof = proofArray.getJSONObject(0);

        //proofArray.remove(0);
        //JSONObject NULLObkect=null;
        //proofArray.put(partofproof.put("timestamp",timestampProofReq));
        String proof = new JSONObject(proffCreation.proofJson).put("identifiers",proofArray).toString(4);
        String deltaForProof = Studente.getDeltaForProof(revreg, demo2.getUnixTimeStamp());
        String revocRegs = new JSONObject().put(revreg.revRegId, new JSONObject().put(Long.toString(timestampProofReq), new JSONObject(getDeltaForProof))).toString();
        String revocRegDefs=new JSONObject().put(revreg.revRegId, new JSONObject(Studente.getRevocationDefinition(revreg.revRegId))).toString();

        System.out.println(credDefStudIdent);
        System.out.println("printing all the way:"+
                "\n createdProof.request 1: "+proofReqbody+"\n createdProof proof 2 :" + proof
                +"\n createdProof proof 3:" + proffCreation.schemas+
                "+\n created proof 4:"+ proffCreation.credentialDefs+
                "\n revocregdegs 5: "+ revocRegDefs +
                "\n revocReg 6:"+ revocRegs);
        /*
        University.IssuerRevokeCredentialAndPublish(revreg,credential.revocId);
        TEST revocation after proof request timestamp, as expected the proof will be valid

        try {
            sleep((3*1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        System.out.println("proof is right? "+
                Spotify.returnVerifierVerifyProof(proofReqbody,proof,proffCreation.schemas,proffCreation.credentialDefs
                        ,revocRegDefs,revocRegs));
        try {
            jsonStoredCred.makeBackup();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
