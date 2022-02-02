package IndyLibraries;

import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults;
import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.pool.Pool;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class stressOfflineTest {
    public static void main(String[] args) {
        File agentsFile = new File("./agentsFile.json");
        JSONUserCredentialStorage jsonStoredCred = null;
        //University DID: L8m6zGD1RLXKD4hbYorhXe University VerKey:BRriQz9X2YiD2aDgyfPfnKnHzPeqhCVMbYbZ1U7YVGPV

        try {
            jsonStoredCred = new JSONUserCredentialStorage(agentsFile);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
        try {
            jsonStoredCred = new JSONUserCredentialStorage(agentsFile);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Pool pool = null;
        Endorser university = null
                ;
        String poolName = "INDYSCANPOOL";
        try{
            Pool.setProtocolVersion(2).get();

        pool = Pool.openPoolLedger(poolName, "{}").get();
        university = new Endorser(pool,"university",jsonStoredCred);
        //University DID: L8m6zGD1RLXKD4hbYorhXe University VerKey:BRriQz9X2YiD2aDgyfPfnKnHzPeqhCVMbYbZ1U7YVGPV
             university.OpenWallet("endorserwallet","abcd");//already endorser
            university.createDID("000000000000000000000000Erenyega");//endorser DID seed
        }
        catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        AnoncredsResults.IssuerCreateSchemaResult createSchemaResult =
                null;
        String schemaId;
        String schemaJson;
        SchemaStructure schemaStructure;
        long accepttime=1643160836; //must be in range.
        try {
            //System.out.println("Do you agree to perform 20 transactions?");
            Scanner sc= new Scanner(System.in);
            //sc.next();
            //String[] attributesForSchema = {"hello" ,"helloworld","helloworld!"};
            //Null data or empty JSON are acceptable here. In this case, ledger will return the latest version of TAA.
            int i;
            /*test sulle cred schema errate
            for (i=0; i<20; i++) {
                university.publishschema("testschemaNumbex", String.valueOf(i),attributesForSchema);
            }*/
            /*
            //test sulle cred schema corrette
            for (i=0; i<20; i++) {
                university.publishschema("testschemaNumbeB", String.valueOf(i)+".0",attributesForSchema);
            }*/
            //get schema + cred def on  existing schema
            /*
            schemaJson="{\"ver\":\"1.0\",\"id\":\"EUw5BvUagdrrYdpKh6DJCQ:2:testschemaNumber:0.0\",\"name\":\"testschemaNumber\",\"version\":\"0.0\",\"attrNames\":[\"hello\",\"helloworld\",\"helloworld!\"],\"seqNo\":355}";
                CreateCredentialResult createCredentialResult = null;
                university.IssuerCreateStoreAndPublishPrefinedSchema("D" + 0, true, "31s62mrw7S2waAbga7n8n9:2:testschemaNumber:18.0", schemaJson);
                /*
            schemaJson="{\"ver\":\"1.0\",\"id\":\"31s62mrw7S2waAbga7n8n9:2:testschemaNumber:18.0\",\"name\":\"testschemaNumber\",\"version\":\"18.0\",\"attrNames\":[\"1\",\"of\",\"attributes\",\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789\",\"lot\"],\"seqNo\":12}";
            for (i=0;i<20;i++){
                university.IssuerCreateStoreAndPublishPrefinedSchema("C"+i,true,"31s62mrw7S2waAbga7n8n9:2:testschemaNumber:18.0",schemaJson);
            }*/
            //university.returnCredentialOffer(createCredentialResult.credentialJson);
            /*
            String[] attribute123ForSchema = {"hello"};
            SchemaStructure schemaStructure1 = university.publishschema("testschemaNumbe5", 1+".0",attribute123ForSchema);

            CredDefStructure credDefStudIdent=
                    university.IssuerCreateStoreAndPublishCredDef("btagB",true,
                            schemaStructure1.schemaId);
            RevocationRegistryObject revreg=university.createRevocationRegistry(null,"Hi",credDefStudIdent.credDefId,
                    "ISSUE_ON_DEMAND","5");
            String revocationRegistry=university.publishRevocationRegistryDefinition(revreg);
            String revRegFirsTEntry = university.publishRevocationRegistryEntry(revreg,revreg.revRegEntryJson, getUnixTimeStamp());


            System.out.println("entry?"+revRegFirsTEntry);
            Agent Studente = new Agent(pool,"Alice",jsonStoredCred);
            //necessario avere un wallet nuovo ad ogni esecuzione al momento
            Studente.CreateWallet("StudentWallet1","passWORD");
            Studente.OpenWallet("StudentWallet1","passWORD");
            Studente.createDID();
            Studente.createMasterSecret();

            CredOfferStructure credOfferToStudent=university.returnCredentialOffer(credDefStudIdent.credDefId);
            System.out.println("dimension of credentialOffer size in bytes measure:"+credOfferToStudent.credOffer.getBytes(StandardCharsets.UTF_8).length+"\n");
            CredRequestStructure credRequestStructure=Studente.returnproverCreateCredentialRequest(credOfferToStudent);
            System.out.println("credential request size in bytes measure:" +credRequestStructure.credReqJson.getBytes(StandardCharsets.UTF_8).length+"\n");
            System.out.println("credential request METADATA size in bytes measure:: \n"+credRequestStructure.credReqMetadataJson.getBytes(StandardCharsets.UTF_8).length+"\n");

            CreateCredentialResult credential=university.returnIssuerCreateCredential(attribute123ForSchema,new String[]{"1"},
                    credOfferToStudent.credOffer, credRequestStructure.credReqJson,revreg
            );

            System.out.println("Revocation Registry entry in bytes measure:"+credential.revocRegDeltaJson.getBytes(StandardCharsets.UTF_8).length);
            university.publishRevocationRegistryEntry(revreg,credential.revocRegDeltaJson, getUnixTimeStamp());

            System.out.println("credential dimension as created by issuermeasure:"+ credential.credentialJson.getBytes(StandardCharsets.UTF_8).length+"\n");
            String credentialStore=Studente.storeCredentialInWallet(null,credOfferToStudent.credDef.credDefId,credRequestStructure.credReqMetadataJson,
                    credential.credentialJson,credOfferToStudent.credDef.credDefJson,revreg.revRegId);
            System.out.println("credential stored return "+ credentialStore);
            System.out.println("credential stored return dimension measure:"+ credentialStore.getBytes(StandardCharsets.UTF_8).length+"\n");

            System.out.println("metrics from agent after first issuing"+ Studente.collectMetrics()+ "\n\n");

            CredOfferStructure credOfferToStudent2=university.returnCredentialOffer(credDefStudIdent.credDefId);
            System.out.println("dimension of credentialOffer size in bytes measure:"+credOfferToStudent2.credOffer.getBytes(StandardCharsets.UTF_8).length+"\n");
            CredRequestStructure credRequestStructure2=Studente.returnproverCreateCredentialRequest(credOfferToStudent2);
            System.out.println("credential request size in bytes measure:" +credRequestStructure2.credReqJson.getBytes(StandardCharsets.UTF_8).length+"\n");
            System.out.println("credential request METADATA size in bytes measure: \n"+credRequestStructure2.credReqMetadataJson.getBytes(StandardCharsets.UTF_8).length+"\n");

            CreateCredentialResult credential2=university.returnIssuerCreateCredential(attribute123ForSchema,new String[]{"1"},
                    credOfferToStudent2.credOffer, credRequestStructure2.credReqJson,revreg
            );
            System.out.println("Revocation Registry entry in bytes measure:"+credential2.revocRegDeltaJson.getBytes(StandardCharsets.UTF_8).length);
            university.publishRevocationRegistryEntry(revreg,credential2.revocRegDeltaJson, getUnixTimeStamp());

            System.out.println("credential dimension as created by issuer measure:"+ credential2.credentialJson.getBytes(StandardCharsets.UTF_8).length+"\n");
            String credentialStore2=Studente.storeCredentialInWallet(null,credOfferToStudent2.credDef.credDefId,credRequestStructure2.credReqMetadataJson,
                    credential2.credentialJson,credOfferToStudent2.credDef.credDefJson,revreg.revRegId);
            System.out.println("credential stored return "+ credentialStore2);
            System.out.println("credential stored return dimension measure:"+ credentialStore2.getBytes(StandardCharsets.UTF_8).length+"\n");

            System.out.println("metrics from agent after second issuing"+ Studente.collectMetrics()+ "\n\n");

            */


            Calendar calendar = Calendar.getInstance();


            String[] attribute123ForSchema = {"hello","abcd"};
            SchemaStructure schemaStructure1 = university.publishschema("ProofTes67chema"+calendar.get(Calendar.SECOND), 5+".0",attribute123ForSchema);
            CredDefStructure credDefStudIdent=
                    university.IssuerCreateStoreAndPublishCredDef("TAGProofTest"+calendar.get(Calendar.SECOND),true,
                            schemaStructure1.schemaId);
            RevocationRegistryObject revreg=university.createRevocationRegistry(null,"Hi",credDefStudIdent.credDefId,
                    "ISSUE_ON_DEMAND","5");
            String revocationRegistry=university.publishRevocationRegistryDefinition(revreg);
            String revRegFirsTEntry = university.publishRevocationRegistryEntry(revreg,revreg.revRegEntryJson, getUnixTimeStamp());

            System.out.println("entry?"+revRegFirsTEntry);
            Agent Studente = new Agent(pool,"Alice",jsonStoredCred);
            //necessario avere un wallet nuovo ad ogni esecuzione al momento
            Studente.CreateWallet("StudentWallet1","passWORD");
            Studente.OpenWallet("StudentWallet1","passWORD");
            Studente.createDID();
            Studente.createMasterSecret();

            CredOfferStructure credOfferToStudent=university.returnCredentialOffer(credDefStudIdent.credDefId);
            System.out.println("dimension of credentialOffer size in bytes measure:"+credOfferToStudent.credOffer.getBytes(StandardCharsets.UTF_8).length+"\n");
            CredRequestStructure credRequestStructure=Studente.returnproverCreateCredentialRequest(credOfferToStudent);
            System.out.println("credential request size in bytes measure:" +credRequestStructure.credReqJson.getBytes(StandardCharsets.UTF_8).length+"\n");
            System.out.println("credential request METADATA size in bytes measure:: \n"+credRequestStructure.credReqMetadataJson.getBytes(StandardCharsets.UTF_8).length+"\n");
            String x="5";
            String y="0";
            if(y.equals("1")) {
                for (i = 0; i < 10; i++) {
                    university.getCredentialDefinitionFromLedger(credDefStudIdent.credDefId);
                }
                for (i = 0; i < 3; i++) {
                    university.getSchemaFromLedger(schemaStructure1.schemaId);
                }
                for (i = 0; i < 8; i++) {
                    university.getDeltaForProof(revreg, getUnixTimeStamp());
                }
            }

            CreateCredentialResult credential=null;
            if(x.equals("4")|| x.equals("7")) {
                credential = university.returnIssuerCreateCredential(attribute123ForSchema, new String[]{"1111111111", "33333"},
                        credOfferToStudent.credOffer, credRequestStructure.credReqJson, revreg
                );
            }else{
                credential = university.returnIssuerCreateCredential(attribute123ForSchema, new String[]{"helloworld", "hello"},
                        credOfferToStudent.credOffer, credRequestStructure.credReqJson, revreg);

            }

            System.out.println("Revocation Registry entry in bytes measure:"+credential.revocRegDeltaJson.getBytes(StandardCharsets.UTF_8).length);
            university.publishRevocationRegistryEntry(revreg,credential.revocRegDeltaJson, getUnixTimeStamp());

            System.out.println("credential dimension as created by issuermeasure:"+ credential.credentialJson.getBytes(StandardCharsets.UTF_8).length+"\n");
            String credentialStore=Studente.storeCredentialInWallet(null,credOfferToStudent.credDef.credDefId,credRequestStructure.credReqMetadataJson,
                    credential.credentialJson,credOfferToStudent.credDef.credDefJson,revreg.revRegId);
            System.out.println("credential stored return "+ credentialStore);
            System.out.println("credential stored return dimension measure:"+ credentialStore.getBytes(StandardCharsets.UTF_8).length+"\n");

            System.out.println("metrics from agent after first issuing"+ Studente.collectMetrics()+ "\n\n");


            ArrayList requestedrevealed = null;

            Endorser Spotify = new Endorser(pool,"Spotify",jsonStoredCred);
            Spotify.CreateWallet("SpotifyWallet","SpassWORD");
            Spotify.OpenWallet("SpotifyWallet","SpassWORD");
            Spotify.createDID();
            long timestampProofReq= getUnixTimeStamp();
            JSONObject attributeGenerated = null,attributeGenerated2=null,attributeGenerated3=null;
            JSONObject predicateGenerated=null,predicateGenerated2=null;
            if(x.equals("4")|| x.equals("7")){
                predicateGenerated = Spotify.generatePredicatesInfoForProofRequest("hello",">","1",schemaStructure1.schemaId,
                        null,
                        null,
                        null,
                        null, null,null, timestampProofReq, timestampProofReq
                        );
            }
            if(x.equals("20")){
                attributeGenerated = Spotify.generateAttrInfoForProofRequest(null,
                        new String[]{"hello","abcd"},
                        null,null,null,null,
                        university.mainDID.didName,credDefStudIdent.credDefId,null,
                       null,null,timestampProofReq,timestampProofReq);

            }
            if(x.equals("7")){
                predicateGenerated2 = Spotify.generatePredicatesInfoForProofRequest("abcd",">","1",schemaStructure1.schemaId,
                        null,
                        null,
                        null,
                        null, null,null, timestampProofReq, timestampProofReq
                );

            }

            if(x.equals("1")||x.equals("5")) {
                attributeGenerated = Spotify.generateAttrInfoForProofRequest("hello", null,
                        schemaStructure1.schemaId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null, null, null, timestampProofReq, timestampProofReq);
            }
            if(x.equals("2")) {
                attributeGenerated2 = Spotify.generateAttrInfoForProofRequest("hello", null,
                        schemaStructure1.schemaId,
                        null,
                        null,
                        null,
                        null,
                        credDefStudIdent.credDefId,
                        null, null, null, timestampProofReq, timestampProofReq);
            }
            if(x.equals("5")) {
                attributeGenerated2 = Spotify.generateAttrInfoForProofRequest("abcd", null,
                        schemaStructure1.schemaId,
                        null,
                        null,
                        null,
                        null,
                        credDefStudIdent.credDefId,
                        null, null, null, timestampProofReq, timestampProofReq);
            }

            String proofReqbody=null;
            if(x.equals("1")){
                proofReqbody =Spotify.returnVerifierGenerateProofRequest("reproof1","1.0","1.0",
                        new JSONObject[]{attributeGenerated}
                        ,null,timestampProofReq,timestampProofReq);
            }
            if(x.equals("20")){
                proofReqbody =Spotify.returnVerifierGenerateProofRequest("reproof1","1.0","1.0",
                        new JSONObject[]{attributeGenerated}
                        ,null,timestampProofReq,timestampProofReq);

                //DURING PROOF CREATION IF THE REQUESTED ATTRIBUTES ARE UNREVEALED IT CAUSES AN EXCEPTION
                //IT SEEMS THAT ATTRIBUTES THAT GO with the names:"" in the proof request MUST be presented in a
                //revealed form
                //as in the proof field the proof there is only the field "revealed_attr_groups" for groups of attributes.
                requestedrevealed=new ArrayList();
            requestedrevealed.add("attr0_referent");
            ProofAttributesFetched proofStruct=Studente.returnProverSearchAttrForProof(proofReqbody,requestedrevealed);


            }

            if(x.equals("5")){
                proofReqbody =Spotify.returnVerifierGenerateProofRequest("reproof1","1.0","1.0",
                        new JSONObject[]{attributeGenerated,attributeGenerated2}
                        ,null,timestampProofReq,timestampProofReq);
            }

             if (x.equals("2")){
                proofReqbody =Spotify.returnVerifierGenerateProofRequest("reproof1","1.0","1.0",
                        new JSONObject[]{attributeGenerated2}
                        ,null,timestampProofReq,timestampProofReq);
            }
             if(x.equals("4")){
                proofReqbody =Spotify.returnVerifierGenerateProofRequest("reproof1","1.0","1.0",
                        null,new JSONObject[]{predicateGenerated},timestampProofReq,timestampProofReq);
            }
             if(x.equals("7")){
                proofReqbody =Spotify.returnVerifierGenerateProofRequest("reproof1","1.0","1.0",
                        null,new JSONObject[]{predicateGenerated,predicateGenerated2},timestampProofReq,timestampProofReq);

            }
            System.out.println("proof request (predicat!!) in bytes measure"+ proofReqbody.getBytes(StandardCharsets.UTF_8).length +"\n\n");
            ProofAttributesFetched proofStruct=Studente.returnProverSearchAttrForProof(proofReqbody,requestedrevealed);
            ProofCreationExtra proffCreation= Studente.proverCreateProof(proofStruct,proofReqbody,null,timestampProofReq, revreg.blobStorageReaderHandle);
            System.out.println("the proof\n"+proffCreation.proofJson);
            System.out.println("prooflenght measure (predicate!!) in bytes measure"+proffCreation.proofJson.length());
            JSONArray proofArray = new JSONObject(proffCreation.proofJson).getJSONArray("identifiers");
            String proof = new JSONObject(proffCreation.proofJson).put("identifiers",proofArray).toString(4);
            String deltaForProof = Studente.getDeltaForProof(revreg, getUnixTimeStamp());
            System.out.println("revocation registry delta from ledger dimensione proof measure:"+deltaForProof.getBytes(StandardCharsets.UTF_8).length);
            String revocRegs = new JSONObject().put(revreg.revRegId, new JSONObject().put(Long.toString(timestampProofReq), new JSONObject(deltaForProof))).toString();
            String revocRegDefs=new JSONObject().put(revreg.revRegId, new JSONObject(Studente.getRevocationDefinition(revreg.revRegId))).toString();

            System.out.println("proof is right (NO RESTRICTIONS!))? "+
                    Spotify.returnVerifierVerifyProof(proofReqbody,proof,proffCreation.schemas,proffCreation.credentialDefs,revocRegDefs,revocRegs));
            Thread.sleep(3999);
            System.out.println("metrics from agent after second issuing"+ Studente.collectMetrics()+ "\n\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static long getUnixTimeStamp() {
        return Instant.now().getEpochSecond();
    }

}

