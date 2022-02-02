package IndyLibraries;

import org.apache.commons.io.FileUtils;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.anoncreds.Anoncreds;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults;
import org.hyperledger.indy.sdk.blob_storage.BlobStorageReader;
import org.hyperledger.indy.sdk.blob_storage.BlobStorageWriter;
import org.hyperledger.indy.sdk.ledger.Ledger;
import org.hyperledger.indy.sdk.ledger.LedgerResults;
import org.hyperledger.indy.sdk.pool.Pool;
import org.json.JSONObject;

import javax.crypto.NoSuchPaddingException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

import static org.hyperledger.indy.sdk.ledger.Ledger.*;


public class Endorser extends Agent {
    String transactionAuthotAgreement;
    String taaAcceptanceMechanism;

    public Endorser(Pool poolConnection, String agentName, JSONUserCredentialStorage agentsFile) {
        super(poolConnection, agentName,agentsFile);
    }

    public String askForEndorserRoleRequest(boolean isTrustAnchor) {
        String trustAnchor;
        if(isTrustAnchor == true){
            trustAnchor="true";
        }
        else trustAnchor="false";
        return IndyJsonStringBuilder.create_nym_endorserRequest(this.mainDID.didName,this.mainDID.didVerKey,trustAnchor);
    }

    //publish schema to ledger, returns (SchemaId,SchemaJson) in the ledger and add (SchemaId,SchemaJson) to this IndyLibraries.Agent IndyLibraries.Endorser
    //Schema collection
    public SchemaStructure publishschema(String schemaName, String schemaVersion, String[] schemaAttributes){
        long preSubmit,postSumbit;
        AnoncredsResults.IssuerCreateSchemaResult createSchemaResult =
                null;
        String schemaId;
        String schemaJson;
        SchemaStructure schemaStructure;
        try {

            createSchemaResult = Anoncreds.issuerCreateSchema(this.mainDID.didName, schemaName, schemaVersion,
                    IndyJsonStringBuilder.createSchemaAttributesString(schemaAttributes)).get();
            String schemaRequest = Ledger.buildSchemaRequest(this.mainDID.didName, createSchemaResult.getSchemaJson()).get();
            preSubmit=System.currentTimeMillis();
            String s=Ledger.signAndSubmitRequest(this.poolConnection, this.mainWallet, this.mainDID.didName, schemaRequest).get();
            postSumbit=System.currentTimeMillis();
            System.out.println("SCHEMA transaction local presubmit: "+ preSubmit + " postsubmit: "+postSumbit +
                    " delta:"+ (postSumbit-preSubmit) );
            System.out.println("result of transaction create schame: \n"+s);

        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        } catch (IndyException e) {
            e.printStackTrace();
            return null;
        }
        if(createSchemaResult!=null) {
            schemaId = createSchemaResult.getSchemaId();
            schemaJson = createSchemaResult.getSchemaJson();
            schemaStructure=new SchemaStructure(schemaId,schemaName,schemaJson,schemaAttributes,schemaVersion);
            this.SchemaCollection.put(schemaId,schemaStructure);
            return schemaStructure;
        }
        return null;
    }
    public SchemaStructure publishschemaOnSovrin(String schemaName, String schemaVersion, String[] schemaAttributes,
                                                 String taaDigest,
                                                 String taaAcceptanceMechanism, Long acceptanceTimestamp){

        AnoncredsResults.IssuerCreateSchemaResult createSchemaResult =
                null;
        String schemaId;
        String schemaJson;
        SchemaStructure schemaStructure;
        String appendedTTARequest;
        Long preSubmit,postSubmit;
        String s;
        try {

            createSchemaResult = Anoncreds.issuerCreateSchema(this.mainDID.didName, schemaName, schemaVersion,
                    IndyJsonStringBuilder.createSchemaAttributesString(schemaAttributes)).get();
            String schemaRequest = Ledger.buildSchemaRequest(this.mainDID.didName, createSchemaResult.getSchemaJson()).get();
            appendedTTARequest = appendTxnAuthorAgreementAcceptanceToRequest(schemaRequest, null, null, taaDigest,
                    taaAcceptanceMechanism, acceptanceTimestamp).get();
            preSubmit=System.currentTimeMillis();
            s=Ledger.signAndSubmitRequest(this.poolConnection,this.mainWallet,this.mainDID.didName,appendedTTARequest).get();
            //System.out.println("string submitted cred def "+ s);
            postSubmit=System.currentTimeMillis();
            System.out.println("Schema Definition on Sovrin presubmit: "+ preSubmit + " postsubmit: "+postSubmit +
                    " delta:"+ (postSubmit-preSubmit) );
            System.out.println("result of transaction create schame: \n"+s);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        } catch (IndyException e) {
            e.printStackTrace();
            return null;
        }
        if(createSchemaResult!=null) {
            schemaId = createSchemaResult.getSchemaId();
            schemaJson = createSchemaResult.getSchemaJson();
            schemaStructure=new SchemaStructure(schemaId,schemaName,schemaJson,schemaAttributes,schemaVersion);
            this.SchemaCollection.put(schemaId,schemaStructure);
            return schemaStructure;
        }
        return null;
    }

    public String publishRevocationRegistryDefinition( RevocationRegistryObject revocationRegistry){
        //blocco fondamentale : revocation Registry definition viene definita solo quando , si aggiunge ad una
        //credential definition già pubblicata senza revocation registry o non con questo nuovo revocation registry

        AnoncredsResults.IssuerCreateSchemaResult createSchemaResult =
                null;
        String schemaId;
        String schemaJson;
        String s= null;
        try {

            String revRegistryRequest = Ledger.buildRevocRegDefRequest(this.mainDID.didName,revocationRegistry.revRegDefJson).get();
            s=Ledger.signAndSubmitRequest(this.poolConnection, this.mainWallet, this.mainDID.didName, revRegistryRequest).get();
            //System.out.println("string submitted "+ s);
            return s;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        } catch (IndyException e) {
            e.printStackTrace();
            return null;
        }
    }
    public String publishRevocationRegistryEntry(RevocationRegistryObject revocationRegistry, String
                                                  registryDelta,long time){

        //Publish on ledger a revocation registry delta entry.
        AnoncredsResults.IssuerCreateSchemaResult createSchemaResult =
                null;
        String schemaJson;
        SchemaStructure schemaStructure;
        LedgerResults.ParseResponseResult publishRevRegResult=null;
        String s= null;
        String toRevokeee;
        try {

            Anoncreds.createRevocationState(revocationRegistry.blobStorageReaderHandle,revocationRegistry.revRegDefJson,
                    registryDelta,
                    time,revocationRegistry.revRegDefJson);
            toRevokeee=Ledger.buildRevocRegEntryRequest(this.mainDID.didName,revocationRegistry.revRegId,
                    "CL_ACCUM",registryDelta).
                    get();
            String revRegistryRequest = Ledger.buildRevocRegDefRequest(this.mainDID.didName,revocationRegistry.revRegDefJson).get();
            System.out.println(signAndSubmitRequest(this.poolConnection,this.mainWallet,
                    this.mainDID.didName,revRegistryRequest).get());
            s=Ledger.signAndSubmitRequest(this.poolConnection, this.mainWallet, this.mainDID.didName, toRevokeee).get();
            //System.out.println("string submitted "+ s);
            return s;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        } catch (IndyException e) {
            e.printStackTrace();
            return null;
        }
    }

    public CredDefStructure IssuerCreateStoreAndPublishPrefinedSchema(String credDefTag,boolean supportRevocation,String schemaID,String schemaJson){
        String credDefId,credDefJson,credDefRequest;
        CredDefStructure credDefStructure;
        AnoncredsResults.IssuerCreateAndStoreCredentialDefResult credDefResult = null;
        String credDefConfigJson =IndyJsonStringBuilder.typeSpecificConfigCredDef(supportRevocation);
        //search for schemaJson
        String appendedTTARequest;
        SchemaStructure schemaStructure=this.SchemaCollection.get(schemaID);
        Long preSubmit,postSubmit;
        try {
            System.out.println("is wallet null" + this.mainWallet == null);
            //NOTE: the method issuerCreateAndStoreCredentialDef will encapsulate the secret keys for issuing cred
            //and revoking credential,so that only the cred_def creator can issuer it and revoke it
            //in this case we have verkey+ pair of keys of signign, different from the one used to authenticate the
            //issuer DID
            credDefResult=
                    Anoncreds.issuerCreateAndStoreCredentialDef(this.mainWallet, this.mainDID.didName, schemaJson, credDefTag, null,   credDefConfigJson).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        } catch (IndyException e) {
            e.printStackTrace();
            return null;
        }
        if(credDefResult!=null) {
            credDefId = credDefResult.getCredDefId();
            credDefJson = credDefResult.getCredDefJson();
            //System.out.println("cred ded json"+ credDefJson);
            String s;
            //publish credef
            try {

                credDefRequest = Ledger.buildCredDefRequest(this.mainDID.didName, credDefJson).get();
                for (int i = 0; i < 20; i++) {
                preSubmit = System.currentTimeMillis();
                s = Ledger.signAndSubmitRequest(this.poolConnection, this.mainWallet, this.mainDID.didName, credDefRequest).get();
                //System.out.println("string submitted cred def "+ s);
                postSubmit = System.currentTimeMillis();
                System.out.println("CLAIM_DEF transaction local presubmit: " + preSubmit + " postsubmit: " + postSubmit +
                        " delta:" + (postSubmit - preSubmit));
                    System.out.println("result of transaction create cred def: \n"+s);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            } catch (ExecutionException e) {
                e.printStackTrace();
                return null;
            } catch (IndyException e) {
                e.printStackTrace();
                return null;
            }
            //storing credef structure
            credDefStructure = new CredDefStructure(credDefId,credDefJson);
            this.CredDefsCollection.put(credDefId,credDefStructure);

            return credDefStructure;
        }
        return null;
    }

    public CredDefStructure IssuerCreateStoreAndPublishCredDef(String credDefTag,boolean supportRevocation,String schemaID){
        String credDefId,credDefJson,credDefRequest;
        CredDefStructure credDefStructure;
        AnoncredsResults.IssuerCreateAndStoreCredentialDefResult credDefResult = null;
        String credDefConfigJson =IndyJsonStringBuilder.typeSpecificConfigCredDef(supportRevocation);
        //search for schemaJson
        String schemaJson;
        SchemaStructure schemaStructure=this.SchemaCollection.get(schemaID);
        long pre,post;
        if (schemaStructure!=null)
            schemaJson=schemaStructure.schemaJson;
        else
            schemaJson=getSchemaFromLedger(schemaID);
        AnoncredsResults.IssuerCreateAndStoreCredentialDefResult createCredDefResult;
        try {
            String getSchemaRequest=Ledger.buildGetSchemaRequest(this.mainDID.didName,
                    schemaID).get();//la differenza tra metodo per creare e ottenere è 'GET'
            //System.out.println("schema id -ed- "+schemaID);
            pre=System.currentTimeMillis();
            String s=Ledger.submitRequest(this.poolConnection,getSchemaRequest).get();
            post = System.currentTimeMillis();
            System.out.println("GetSchemaReq pre:"+ pre+" post:"+post+" delta: "+(post-pre) );
            //necessario parsare lo schema ottenuto in uno frienldy con AnonCreds API e issuer credential
            LedgerResults.ParseResponseResult responseScheme = parseGetSchemaResponse(s).get();

            schemaJson=responseScheme.getObjectJson();

            System.out.println("is wallet null" + this.mainWallet == null);
            //NOTE: the method issuerCreateAndStoreCredentialDef will encapsulate the secret keys for issuing cred
            //and revoking credential,so that only the cred_def creator can issuer it and revoke it
            //in this case we have verkey+ pair of keys of signign, different from the one used to authenticate the
            //issuer DID
            credDefResult=
            Anoncreds.issuerCreateAndStoreCredentialDef(this.mainWallet, this.mainDID.didName, schemaJson, credDefTag, null,   credDefConfigJson).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        } catch (IndyException e) {
            e.printStackTrace();
            return null;
        }
        if(credDefResult!=null) {
            credDefId = credDefResult.getCredDefId();
            credDefJson = credDefResult.getCredDefJson();
            //System.out.println("cred ded json"+ credDefJson);

            //publish credef
            try {
                credDefRequest = Ledger.buildCredDefRequest(this.mainDID.didName,credDefJson).get();
                //System.out.println("\ncred def def "+credDefRequest);
                pre =System.currentTimeMillis();
                String s=Ledger.signAndSubmitRequest(this.poolConnection,this.mainWallet,this.mainDID.didName,credDefRequest).get();
                post =System.currentTimeMillis();
                System.out.println("cred def pre: "+pre+" post:"+ post+" delta: "+ (post-pre));
                //System.out.println("string submitted cred def "+ s);

            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            } catch (ExecutionException e) {
                e.printStackTrace();
                return null;
            } catch (IndyException e) {
                e.printStackTrace();
                return null;
            }
            //storing credef structure
            credDefStructure = new CredDefStructure(credDefId,credDefJson);
            this.CredDefsCollection.put(credDefId,credDefStructure);

            return credDefStructure;
        }
        return null;
    }
    public CredDefStructure IssuerCreateStoreAndPublishCredDefOnSovrin(String credDefTag,boolean supportRevocation,String schemaID,String taaDigest,
                                                               String taaAcceptanceMechanism, Long acceptanceTimestamp){
        String credDefId,credDefJson,credDefRequest;
        CredDefStructure credDefStructure;
        AnoncredsResults.IssuerCreateAndStoreCredentialDefResult credDefResult = null;
        String credDefConfigJson =IndyJsonStringBuilder.typeSpecificConfigCredDef(supportRevocation);
        //search for schemaJson
        String schemaJson;
        String appendedTTARequest;
        SchemaStructure schemaStructure=this.SchemaCollection.get(schemaID);
        Long preSubmit,postSubmit;
        if (schemaStructure!=null)
            schemaJson=schemaStructure.schemaJson;
        else
            schemaJson=getSchemaFromLedger(schemaID);
        AnoncredsResults.IssuerCreateAndStoreCredentialDefResult createCredDefResult;
        try {
            String getSchemaRequest=Ledger.buildGetSchemaRequest(this.mainDID.didName,
                    schemaID).get();//la differenza tra metodo per creare e ottenere è 'GET'
            //System.out.println("schema id -ed- "+schemaID);
            pre=System.currentTimeMillis();
            String s=Ledger.submitRequest(this.poolConnection,getSchemaRequest).get();
            post =System.currentTimeMillis();
            System.out.println("GET SCHEMA ON SOVRIN pre: "+pre+" post:"+ post+" delta: "+ (post-pre));

            //necessario parsare lo schema ottenuto in uno frienldy con AnonCreds API e issuer credential
            LedgerResults.ParseResponseResult responseScheme = parseGetSchemaResponse(s).get();

            schemaJson=responseScheme.getObjectJson();

            System.out.println("is wallet null" + this.mainWallet == null);
            //NOTE: the method issuerCreateAndStoreCredentialDef will encapsulate the secret keys for issuing cred
            //and revoking credential,so that only the cred_def creator can issuer it and revoke it
            //in this case we have verkey+ pair of keys of signign, different from the one used to authenticate the
            //issuer DID
            System.out.println(schemaJson);
            credDefResult=
                    Anoncreds.issuerCreateAndStoreCredentialDef(this.mainWallet, this.mainDID.didName, schemaJson, credDefTag, null,   credDefConfigJson).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        } catch (IndyException e) {
            e.printStackTrace();
            return null;
        }
        if(credDefResult!=null) {
            credDefId = credDefResult.getCredDefId();
            credDefJson = credDefResult.getCredDefJson();
            //System.out.println("cred ded json"+ credDefJson);

            //publish credef
            try {

                credDefRequest = Ledger.buildCredDefRequest(this.mainDID.didName,credDefJson).get();
                //System.out.println("\ncred def def "+credDefRequest);
                appendedTTARequest = appendTxnAuthorAgreementAcceptanceToRequest(credDefRequest, null, null, taaDigest,
                        taaAcceptanceMechanism, acceptanceTimestamp).get();
                preSubmit=System.currentTimeMillis();
                String s=Ledger.signAndSubmitRequest(this.poolConnection,this.mainWallet,this.mainDID.didName,appendedTTARequest).get();
                //System.out.println("string submitted cred def "+ s);
                postSubmit=System.currentTimeMillis();
                System.out.println("Credential Definition on Sovrin presubmit: "+ preSubmit + " postsubmit: "+postSubmit +
                        " delta:"+ (postSubmit-preSubmit) );
                System.out.println("result of transaction create cred def: \n"+s);

            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            } catch (ExecutionException e) {
                e.printStackTrace();
                return null;
            } catch (IndyException e) {
                e.printStackTrace();
                return null;
            }
            //storing credef structure
            credDefStructure = new CredDefStructure(credDefId,credDefJson);
            this.CredDefsCollection.put(credDefId,credDefStructure);

            return credDefStructure;
        }
        return null;
    }

    public CredDefStructure IssuerCreateStoreAndPublishPrefinedSchemaCredDefOnSovrin(String credDefTag,boolean supportRevocation,String schemaID,String taaDigest,
                                                                       String taaAcceptanceMechanism, Long acceptanceTimestamp,String schemaJson){
        String credDefId,credDefJson,credDefRequest;
        CredDefStructure credDefStructure;
        AnoncredsResults.IssuerCreateAndStoreCredentialDefResult credDefResult = null;
        String credDefConfigJson =IndyJsonStringBuilder.typeSpecificConfigCredDef(supportRevocation);
        //search for schemaJson
        String appendedTTARequest;
        SchemaStructure schemaStructure=this.SchemaCollection.get(schemaID);
        Long preSubmit,postSubmit;
        try {
            System.out.println("is wallet null" + this.mainWallet == null);
            //NOTE: the method issuerCreateAndStoreCredentialDef will encapsulate the secret keys for issuing cred
            //and revoking credential,so that only the cred_def creator can issuer it and revoke it
            //in this case we have verkey+ pair of keys of signign, different from the one used to authenticate the
            //issuer DID
            credDefResult=
                    Anoncreds.issuerCreateAndStoreCredentialDef(this.mainWallet, this.mainDID.didName, schemaJson, credDefTag, null,   credDefConfigJson).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        } catch (IndyException e) {
            e.printStackTrace();
            return null;
        }
        if(credDefResult!=null) {
            credDefId = credDefResult.getCredDefId();
            credDefJson = credDefResult.getCredDefJson();
            //System.out.println("cred ded json"+ credDefJson);

            //publish credef
            try {

                credDefRequest = Ledger.buildCredDefRequest(this.mainDID.didName,credDefJson).get();
                //System.out.println("\ncred def def "+credDefRequest);
                appendedTTARequest = appendTxnAuthorAgreementAcceptanceToRequest(credDefRequest, null, null, taaDigest,
                        taaAcceptanceMechanism, acceptanceTimestamp).get();
                preSubmit=System.currentTimeMillis();
                String s=Ledger.signAndSubmitRequest(this.poolConnection,this.mainWallet,this.mainDID.didName,appendedTTARequest).get();
                //System.out.println("string submitted cred def "+ s);
                postSubmit=System.currentTimeMillis();
                System.out.println("Credential Definition on Sovrin presubmit: "+ preSubmit + " postsubmit: "+postSubmit +
                        " delta:"+ (postSubmit-preSubmit) );
                System.out.println("result of transaction create cred def: \n"+s);

            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            } catch (ExecutionException e) {
                e.printStackTrace();
                return null;
            } catch (IndyException e) {
                e.printStackTrace();
                return null;
            }
            //storing credef structure
            credDefStructure = new CredDefStructure(credDefId,credDefJson);
            this.CredDefsCollection.put(credDefId,credDefStructure);

            return credDefStructure;
        }
        return null;
    }

    //return credential offer to prover
    //Create a Credential Offer fetching a credential definition from the Issuer Wallet, Indy provides the
    //schema_ID, cred_def_id and key correcteness proof about the cred_def_id, additionally in this implementation
    //it is given also the whole credential definition, all the infomration retrivied is stored in a IndyLibraries.CredOfferStructure
    public CredOfferStructure returnCredentialOffer(String credDefId){
        String credentialOffer = null;
        try {
            credentialOffer = Anoncreds.issuerCreateCredentialOffer(this.mainWallet,credDefId).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        CredDefStructure toRet=this.CredDefsCollection.get(credDefId);
        if(toRet==null){
            toRet=new CredDefStructure(credDefId,getCredentialDefinitionFromLedger(credDefId));
        }
        return new CredOfferStructure(toRet,credentialOffer);
    }

    //creates a credential that is non-revocable
    public CreateCredentialResult returnIssuerCreateCredentialNonRevocable(String[] credKeys,String[] credValues,
                                                                           String credOffer,String credReqJson){
        int i;

        if(credKeys.length!=credValues.length){
            throw new IllegalArgumentException();
        }
        //String credValuesJson =IndyJsonStringBuilder.createCredentialValues(credKeys,credValues);

        AnoncredsResults.IssuerCreateCredentialResult createCredentialResult =
                null;
        try {
            JSONObject jsonObjectAttr = new JSONObject();
            // Encoded value of non-integer attribute is SHA256 converted to decimal
            // note that encoding is not standardized by Indy except that 32-bit integers are encoded as themselves. IS-786
            //Encoding of value fields can be a problem if the  256 bit limit is not respected.
            String pass = "password";

            //MessageDigest messageDigest = MessageDigest.getInstance("SHA-256"); it would cause erros someway
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-224");

            for (i=0;i< credKeys.length;i++) {
                String hashStr;
                if(isNumeric(credValues[i])){
                    hashStr=credValues[i];
                    jsonObjectAttr
                            .put(credKeys[i], new JSONObject().put("raw", credValues[i]).put("encoded",credValues[i]));

                }
                else {
                    byte hashBytes[] = messageDigest.digest(credValues[i].getBytes(StandardCharsets.UTF_8));
                    BigInteger noHash = new BigInteger(1, hashBytes);
                    hashStr = noHash.toString(16);
                    jsonObjectAttr
                            .put(credKeys[i], new JSONObject().put("raw", credValues[i]).put("encoded","1234"));
                    //FOR convenience non-numeric will be encoded with value "1234"

                }


            }
            System.out.println(jsonObjectAttr.toString(4));
            System.out.println("credOffer "+credOffer);
            System.out.println("credReq "+credReqJson);
            /*
            IssuerCreateCredentialResult> issuerCreateCredential(Wallet wallet,String credOfferJson,String credReqJson,
            String credValuesJson,String revRegId,int blobStorageReaderHandle )*/
            createCredentialResult = Anoncreds.issuerCreateCredential(this.mainWallet, credOffer, credReqJson, jsonObjectAttr.toString(), null,
                    -1).get();
        } catch (InterruptedException e) {
            System.out.println(e);
            e.printStackTrace();
        } catch (ExecutionException e) {
            System.out.println(e);

            e.printStackTrace();
        } catch (IndyException e) {
            System.out.println(e);

            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e);

            e.printStackTrace();
        }
        if(createCredentialResult!=null) {

            return new CreateCredentialResult(createCredentialResult.getCredentialJson(),createCredentialResult.getRevocId(),
                    createCredentialResult.getRevocRegDeltaJson());
        }
        return null;
    }
    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            int d = Integer.parseInt(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public RevocationRegistryObject createRevocationRegistry(
                                           String revocDefType,String thisRevocationTAG,String credDefID,
                                           String issuranceType,String maxCredRegistryProcess)  {
        String jsonConfig=IndyJsonStringBuilder.createRevocationRegistryConfig(issuranceType,Integer.parseInt(maxCredRegistryProcess));
        String tailsWriterConfig = new JSONObject()
                .put("base_dir", getIndyHomePath("tails").replace('\\', '/'))
                .put("uri_pattern", "")
                .toString();
        System.out.println("See tailsWriterConfig "+tailsWriterConfig);
        BlobStorageWriter tailsWriter = null;
        try {
            tailsWriter = BlobStorageWriter.openWriter("default", tailsWriterConfig).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        System.out.println(jsonConfig);
        System.out.println("creadefoews " + credDefID);
        String revRegDefConfig = new JSONObject()
                .put("issuance_type", "ISSUANCE_ON_DEMAND")
                .put("max_cred_num", 5)
                .toString();
        //String revRegDefConfig = jsonConfig;
        AnoncredsResults.IssuerCreateAndStoreRevocRegResult createAndStoreRevocRegResult=null;
        try {
             createAndStoreRevocRegResult =
                    Anoncreds.issuerCreateAndStoreRevocReg(mainWallet, this.mainDID.didName, revocDefType, thisRevocationTAG, credDefID,
                            revRegDefConfig, tailsWriter).get();
        } catch (IndyException e) {
        } catch (ExecutionException e) {
            System.out.println("Error while creating Revocation Registry, check that the Credential Definition " +
                    "identifified  by cred_id supports revocation ");
            e.printStackTrace();
        } catch (InterruptedException e) {}
        //10. Issuer open Tails Reader
        BlobStorageReader blobStorageReaderCfg = null;
        try {
            blobStorageReaderCfg = BlobStorageReader.openReader("default", tailsWriterConfig).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        int blobStorageReaderHandle = blobStorageReaderCfg.getBlobStorageReaderHandle();
        return new RevocationRegistryObject(createAndStoreRevocRegResult.getRevRegId(),createAndStoreRevocRegResult.getRevRegEntryJson(),
                createAndStoreRevocRegResult.getRevRegDefJson(),blobStorageReaderCfg,blobStorageReaderHandle);


    }

    //This method creates a revocation registry delta with the credential specified by cred_revoc_id
    //revoked, it then can be sent by a REVOC_REG_ENTRY with the method publishRevocationRegistryEntry
    //to the Ledger
    public String IssuerRevokeCredential(RevocationRegistryObject revocationRegistryObject,
                                                          String credentialToRevoke_cred_revoc_ID){
        try {
            //return a string to submit as a REVOC_REG_ENTRY transaction,è una revoc registry entry,
            //possibile accumularne più di una per ridurre il load del ledger(nello scrivere le modificche)
            return Anoncreds.issuerRevokeCredential(this.mainWallet,revocationRegistryObject.blobStorageReaderHandle,
                    revocationRegistryObject.revRegId,credentialToRevoke_cred_revoc_ID).get();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
    public CreateCredentialResult returnIssuerCreateCredential(String[] credKeys,String[] credValues,String credOffer,String credReqJson,
                                                               RevocationRegistryObject revRegistryStruct){
        //IF THIS METHOD GIVES AN ERROR PROMPTLY REPLACE THE ATTRIBUTE VALUE DIGEST WITH A PLACEHOLDER es:12345
        int i;

        if(credKeys.length!=credValues.length){
            throw new IllegalArgumentException();
        }
        //String credValuesJson =IndyJsonStringBuilder.createCredentialValues(credKeys,credValues);

        AnoncredsResults.IssuerCreateCredentialResult createCredentialResult =
                null;
        try {
            JSONObject jsonObjectAttr = new JSONObject();
            // Encoded value of non-integer attribute is SHA256 converted to decimal
            // note that encoding is not standardized by Indy except that 32-bit integers are encoded as themselves. IS-786
            //Encoding of value fields can be a problem if the  256 bit limit is not respected
            String pass = "password";

            //MessageDigest messageDigest = MessageDigest.getInstance("SHA-256"); it would cause erros someway
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-224");
            for (i=0;i< credKeys.length;i++) {
                String hashStr;
                if(isNumeric(credValues[i])){
                    hashStr=credValues[i];
                }
                else {
                    byte hashBytes[] = messageDigest.digest(credValues[i].getBytes(StandardCharsets.UTF_8));
                    BigInteger noHash = new BigInteger(1, hashBytes);
                    hashStr = noHash.toString(16);
                }

                jsonObjectAttr
                        .put(credKeys[i], new JSONObject().put("raw", credValues[i]).put("encoded","1234"));
            }
            createCredentialResult = Anoncreds.issuerCreateCredential(this.mainWallet, credOffer, credReqJson, jsonObjectAttr.toString(4), revRegistryStruct.revRegId, revRegistryStruct.blobStorageReaderHandle).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if(createCredentialResult!=null) {

            return new CreateCredentialResult(createCredentialResult.getCredentialJson(),createCredentialResult.getRevocId(),
                    createCredentialResult.getRevocRegDeltaJson());
        }
        return null;
    }
    public String IssuerMERGERevokeCredential(String merge1,String merge2){
        //when we merge the registries, we have to make sure that rev registry merge1  has value equal to
        //current accum value for revRegDelta merge2.
        //NOTE:revDelta can be obtained by call issuerCreateCredential or issuerRevokeCredential.
        if(merge1 !=null && merge2!=null){
            try {
                return Anoncreds.issuerMergeRevocationRegistryDeltas(merge1,merge2).get();
            } catch (IndyException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    //1)first thing to do after creation
    public String revocationRegistryEntryPublishDelta(RevocationRegistryObject revRegObj){
        String reqToSend,ris;
        long pre,post;
        try {
             reqToSend=Ledger.buildRevocRegEntryRequest(this.mainDID.didName,revRegObj.revRegId,"CL_ACCUM",
                    revRegObj.revRegEntryJson).get();
             pre=System.currentTimeMillis();
            ris=Ledger.signAndSubmitRequest(this.poolConnection,this.mainWallet,this.mainDID.didName,
                    reqToSend).get();
            post =System.currentTimeMillis();
            System.out.println("RegEntryRequest pre: "+pre+" post:"+ post+" delta: "+ (post-pre));

            return ris;
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
    //2)getting accum from the ledger
    public String getRevocRegReqGetAccum(RevocationRegistryObject revocationRegistryObject,long timestamp){
        String request,response;
        long pre,post;
        try {
            request=Ledger.buildGetRevocRegRequest(mainDID.didName, revocationRegistryObject.revRegId,
                    timestamp).get();
            pre = System.currentTimeMillis();
            response = Ledger.submitRequest(this.poolConnection, request).get();
            post = System.currentTimeMillis();
            System.out.println("GetRevocRegRequest pre: "+pre+" post:"+ post+" delta: "+ (post-pre));
            LedgerResults.ParseRegistryResponseResult resultAfterCreatingRevDef = Ledger.parseGetRevocRegResponse(response).get();
            System.out.println("Accum Value at (after creating rev def): " + timestamp + "\n" +  resultAfterCreatingRevDef.getObjectJson() + "\n");
            return resultAfterCreatingRevDef.getObjectJson();

        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
    //3) publish delta, can be done after credential issuing
    //CALL TO -> revocationRegistryEntryPublishDelta
    public String revocationRegistryEntryPublishDelta2(String revID,String delta){
        String reqToSend,ris;
        try {
            reqToSend=Ledger.buildRevocRegEntryRequest(this.mainDID.didName,revID,"CL_ACCUM",
                    delta).get();
            pre = System.currentTimeMillis();
            ris=Ledger.signAndSubmitRequest(this.poolConnection,this.mainWallet,this.mainDID.didName,
                    reqToSend).get();
            post = System.currentTimeMillis();
            System.out.println("RegistryEntryPublishDelta pre: "+pre+" post:"+ post+" delta: "+ (post-pre));
            return ris;
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    //4) READ AGAIN DELTA ACCUM FROM LEDGER ->getRevocRegReqGetAccum
    //5)revoKING!!
    //This method creates a revocation registry delta with the credential specified by cred_revoc_id
    //revoked, it then can be sent by a REVOC_REG_ENTRY with the method publishRevocationRegistryEntry
    //to the Ledger
    public String IssuerRevokeCredentialAndPublish(RevocationRegistryObject revocationRegistryObject,
                                         String credentialToRevoke_cred_revoc_ID){
        String newDeltaPostRevoc,request,response;
        try {
            //return a string to submit as a REVOC_REG_ENTRY transaction,è una revoc registry entry,
            //possibile accumularne più di una per ridurre il load del ledger(nello scrivere le modificche)
            newDeltaPostRevoc=Anoncreds.issuerRevokeCredential(this.mainWallet,revocationRegistryObject.blobStorageReaderHandle,
                    revocationRegistryObject.revRegId,credentialToRevoke_cred_revoc_ID).get();
            request=Ledger.buildRevocRegEntryRequest(this.mainDID.didName,
                    revocationRegistryObject.revRegId, "CL_ACCUM", newDeltaPostRevoc).get();
            pre=System.currentTimeMillis();
            response= Ledger.signAndSubmitRequest(this.poolConnection,this.mainWallet,this.mainDID.didName, request).
                    get();
            post = System.currentTimeMillis();
            System.out.println("RevocRegEntryRequest pre: "+pre+" post:"+ post+" delta: "+ (post-pre));

            System.out.println("The issuer has revoked the credential and published the new accum delta on the ledger\n" + response + "\n");
            return newDeltaPostRevoc;
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
    //6)read again accum


    public  RevocationRegistryDelta getRevocationRegistryDeltaSpecificTimeFrame(RevocationRegistryObject revRegObj,
                                                                                Long from, Long to){
        String request,response;
        request=response=null;
        try {
            request=Ledger.buildGetRevocRegDeltaRequest(this.mainDID.didName,revRegObj.revRegId,from,to).get();
            System.out.println("request form "+request);
            pre = System.currentTimeMillis();
            response=Ledger.signAndSubmitRequest(this.poolConnection,this.mainWallet,
                    this.mainDID.didName,request).get();
            post = System.currentTimeMillis();
            System.out.println("GetRevocRegDeltaRequest pre: "+pre+" post:"+ post+" delta: "+ (post-pre));

            System.out.println("debug response " + response);
            LedgerResults.ParseRegistryResponseResult parseRegistryResponseResult=
                    parseGetRevocRegDeltaResponse(response).get();
            System.out.println("response "+parseRegistryResponseResult.getObjectJson());
            return new RevocationRegistryDelta(parseRegistryResponseResult.getId(),
                    parseRegistryResponseResult.getObjectJson(),
                    parseRegistryResponseResult.getTimestamp());
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getIndyHomePath() {
        return FileUtils.getUserDirectoryPath() + "/.indy_client/";
    }



    public static String getIndyHomePath(String filename) {
        return getIndyHomePath() + filename;
    }

    public static String getTmpPath() {
        return FileUtils.getTempDirectoryPath() + "/indy_client/";
    }

    public static String getTmpPath(String filename) {
        return getTmpPath() + filename;
    }


    //can only add attribute to his own nym
    //must be tested
    public boolean addAttributeToNYM(String[] rawID, String[] rawValues){
        String attribReq;
        String attribResponse;
        String raw = IndyJsonStringBuilder.buildRawAttrJSON(rawID,rawValues);
        try {
            attribReq = Ledger.buildAttribRequest(mainDID.didName,mainDID.didName
                    , null, raw,null).get();
            pre = System.currentTimeMillis();
            attribResponse = signAndSubmitRequest(poolConnection, this.mainWallet, this.mainDID.didName, attribReq).get();
            post = System.currentTimeMillis();
            System.out.println("AttribRequest pre: "+pre+" post:"+ post+" delta: "+ (post-pre));
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
