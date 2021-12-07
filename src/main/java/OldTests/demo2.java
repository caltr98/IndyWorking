package OldTests;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.anoncreds.Anoncreds;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults.IssuerCreateAndStoreCredentialDefResult;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults.IssuerCreateAndStoreRevocRegResult;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults.IssuerCreateCredentialResult;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults.IssuerCreateSchemaResult;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults.ProverCreateCredentialRequestResult;
import org.hyperledger.indy.sdk.blob_storage.BlobStorageReader;
import org.hyperledger.indy.sdk.blob_storage.BlobStorageWriter;
import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.did.DidJSONParameters.CreateAndStoreMyDidJSONParameter;
import org.hyperledger.indy.sdk.did.DidResults.CreateAndStoreMyDidResult;
import org.hyperledger.indy.sdk.ledger.Ledger;
import org.hyperledger.indy.sdk.ledger.LedgerResults.ParseRegistryResponseResult;
import org.hyperledger.indy.sdk.ledger.LedgerResults.ParseResponseResult;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.pool.PoolJSONParameters;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



public class demo2 {

    private static String indyClientPath;

    public static void main(String[] args) throws InterruptedException, ExecutionException, IndyException, IOException {
        String envPath = System.getProperty("user.home");
        indyClientPath = envPath + "/" + ".indy_client";
        File file = new File(indyClientPath);
        if (!file.exists()) {
            file.mkdir();
        }
        System.loadLibrary("indy");
        String[] genesisTransactions = getDefaultGenesisTxn("INDYSCANPOOL");
        writeGenesisTransactions(genesisTransactions, "genesis.txn");
        // create and open wallet
        JSONObject walletConfig = new JSONObject();
        JSONObject walletCred = new JSONObject();
        walletConfig.put("id", "defaultWallet");
        walletCred.put("key", "123");
        Wallet.createWallet(walletConfig.toString(), walletCred.toString()).get();
        Wallet myWallet = Wallet.openWallet(walletConfig.toString(), walletCred.toString()).get();

        // create and open pool with protocol version 2
        Pool.setProtocolVersion(2).get();
        //PoolJSONParameters.CreatePoolLedgerConfigJSONParameter createPoolLedgerConfigJSONParameter = new PoolJSONParameters.CreatePoolLedgerConfigJSONParameter(
          //      indyClientPath + "/" + "genesis.txn");
        //Pool.createPoolLedgerConfig("defaultPool", createPoolLedgerConfigJSONParameter.toJson()).get();
        Pool myPool = Pool.openPoolLedger("INDYSCANPOOL", "{}").get();

        // create steward did from seed
        String seed = "000000000000000000000000Steward1";
        CreateAndStoreMyDidJSONParameter stewardDIDParameter = new CreateAndStoreMyDidJSONParameter(null, seed, null,
                null);
        CreateAndStoreMyDidResult createDidResult = Did.createAndStoreMyDid(myWallet, stewardDIDParameter.toString())
                .get();
        String didSteward = createDidResult.getDid();

        // create trust anchor did and write it to the ledger
        createDidResult = Did.createAndStoreMyDid(myWallet, "{}").get();
        String didTrustAnchor = createDidResult.getDid();
        String keyTrustAnchor = createDidResult.getVerkey();
        String request = Ledger.buildNymRequest(didSteward, didTrustAnchor, keyTrustAnchor, null, "TRUST_ANCHOR").get();
        String response = Ledger.signAndSubmitRequest(myPool, myWallet, didSteward, request).get();


        // trust anchor creates schema and credential definition and writes them to the
        // ledger
        String schemaName = "testschema";
        String version = "1.01";
        JSONArray jsonAttr = new JSONArray();
        jsonAttr.put("licencenumber");
        jsonAttr.put("firstname");
        jsonAttr.put("lastname");
        String schemaAttributes = jsonAttr.toString();
        IssuerCreateSchemaResult schemaResult = Anoncreds
                .issuerCreateSchema(didTrustAnchor, schemaName, version, schemaAttributes).get();
        request = Ledger.buildSchemaRequest(didTrustAnchor, schemaResult.getSchemaJson()).get();
        response = Ledger.signAndSubmitRequest(myPool, myWallet, didTrustAnchor, request).get();
        System.out.println("Write schema to ledger response:\n" + response + "\n");
        // schema has been written to the ledger

        String schemaId = schemaResult.getSchemaId();



        // Trust Anchor writes a credential def to the ledger. He first get the schemadef and schemaid from the ledger
        String schemaRequest = Ledger.buildGetSchemaRequest(didTrustAnchor, schemaId).get();
        String schemResponse = Ledger.submitRequest(myPool, schemaRequest).get();
        ParseResponseResult schemaDefParseResult = Ledger.parseGetSchemaResponse(schemResponse).get();
        String schemaJson = schemaDefParseResult.getObjectJson();
        String schemaDef = schemaJson.toString();

        IssuerCreateAndStoreCredentialDefResult credentialResult = Anoncreds.issuerCreateAndStoreCredentialDef(myWallet,
                didTrustAnchor, schemaDef, "myTag", "CL", "{\"support_revocation\":true}").get();
        request = Ledger.buildCredDefRequest(didTrustAnchor, credentialResult.getCredDefJson()).get();
        response = Ledger.signAndSubmitRequest(myPool, myWallet, didTrustAnchor, request).get();
        System.out.println("Write credential def to ledger response:\n" + response + "\n");
        // credential def has been written to the ledger

        String credDefId = credentialResult.getCredDefId();
        String credDef = credentialResult.getCredDefJson();


        // now the trust anchor creates a revReg and writes the definition to the ledger
        String revocDir = indyClientPath + "/" + "revoc_dir";
        File f = new File(revocDir);
        if (!f.exists()) {
            f.mkdir();
        }
        JSONObject tailsWriterConfig = new JSONObject();
        tailsWriterConfig.put("base_dir", revocDir);
        tailsWriterConfig.put("uri_pattern", "");
        BlobStorageWriter blobWriter = BlobStorageWriter.openWriter("default", tailsWriterConfig.toString()).get();

        JSONObject revRegConfig = new JSONObject();
        revRegConfig.put("issuance_type", "ISSUANCE_ON_DEMAND"); // the other option is ISSUANCE_ON_DEMAND
        revRegConfig.put("max_cred_num", 100);

        IssuerCreateAndStoreRevocRegResult storeRevocResult = Anoncreds.issuerCreateAndStoreRevocReg(myWallet,
                        didTrustAnchor, null, "myTagRevoc", credDefId, revRegConfig.toString(), blobWriter)
                .get();

        request = Ledger.buildRevocRegDefRequest(didTrustAnchor, storeRevocResult.getRevRegDefJson()).get();
        System.out.println("Write revocation def to ledger request:\n" + request + "\n");
        response = Ledger.signAndSubmitRequest(myPool, myWallet, didTrustAnchor, request).get();
        System.out.println("Write revocation def to ledger response:\n" + response + "\n");
        // the revocation def has been written to the ledger

        String revRegDefId = storeRevocResult.getRevRegId();
        String revRegDef = storeRevocResult.getRevRegDefJson();


        // we publish the initial accum value to the ledger
        String intialEntry = storeRevocResult.getRevRegEntryJson();
        String revDefType = "CL_ACCUM ";
        request = Ledger.buildRevocRegEntryRequest(didTrustAnchor, revRegDefId, revDefType, intialEntry).get();
        response = Ledger.signAndSubmitRequest(myPool, myWallet, didTrustAnchor, request).get();
        System.out.println("Write initial accum to ledger response:\n" + response + "\n");


        // read accum from ledger
        long timestampAfterCreatingRevDef = getUnixTimeStamp();
        request = Ledger.buildGetRevocRegRequest(didTrustAnchor, revRegDefId, timestampAfterCreatingRevDef).get();
        response = Ledger.submitRequest(myPool, request).get();
        ParseRegistryResponseResult resultAfterCreatingRevDef = Ledger.parseGetRevocRegResponse(response).get();
        System.out.println("Accum Value at (after creating rev def): " + timestampAfterCreatingRevDef + "\n" +  resultAfterCreatingRevDef.getObjectJson() + "\n");
        //



        // trust anchor issues a credential corresponding to the prior created
        // credential definition and issues it to someone
        JSONObject attributesToIssue = new JSONObject();
        attributesToIssue.put("licencenumber", "L2ZKT17Q2");
        attributesToIssue.put("firstname", "MyFirstNamePhilipp");
        attributesToIssue.put("lastname", "MyLastNameMorrison");

        JSONObject credentialDataForIndy = encode(attributesToIssue);
        String credentialOffer = Anoncreds.issuerCreateCredentialOffer(myWallet, credentialResult.getCredDefId()).get();

        createDidResult = Did.createAndStoreMyDid(myWallet, "{}").get();
        String didProver = createDidResult.getDid();
        String linkSecret = Anoncreds.proverCreateMasterSecret(myWallet, null).get();

        ProverCreateCredentialRequestResult proverCredReqResult = Anoncreds.proverCreateCredentialReq(myWallet,
                didProver, credentialOffer, credentialResult.getCredDefJson(), linkSecret).get();


        BlobStorageReader blobReader = BlobStorageReader.openReader("default", tailsWriterConfig.toString()).get();
        int blobReaderHandle = blobReader.getBlobStorageReaderHandle();


        IssuerCreateCredentialResult createCredResult = Anoncreds.issuerCreateCredential(myWallet, credentialOffer,
                proverCredReqResult.getCredentialRequestJson(), credentialDataForIndy.toString(), revRegDefId, blobReaderHandle).get();
        createCredResult.getRevocId();
        String issuedCredential = createCredResult.getCredentialJson();
        String issuedCredentialDelta = createCredResult.getRevocRegDeltaJson();
        System.out.println("Created credential:\n" + issuedCredential + "\n");
        System.out.println("Created credential delta:\n" + issuedCredentialDelta + "\n");

        // trust anchor publishes the new delta to the ledger
        request = Ledger.buildRevocRegEntryRequest(didTrustAnchor, revRegDefId, revDefType, issuedCredentialDelta).get();
        response = Ledger.signAndSubmitRequest(myPool, myWallet, didTrustAnchor, request).get();
        System.out.println("Issuer writes the delta after issueing a credential to the ledger response \n" + response + "\n");




        // read accum from ledger
        long timestampAfterWritingDeltaAfterIssueingCredential = getUnixTimeStamp();
        request = Ledger.buildGetRevocRegRequest(didTrustAnchor, revRegDefId, timestampAfterWritingDeltaAfterIssueingCredential).get();
        response = Ledger.submitRequest(myPool, request).get();
        ParseRegistryResponseResult resultAfterCredentialIssueing = Ledger.parseGetRevocRegResponse(response).get();
        System.out.println("Accum Value at (after issueing credential): " + timestampAfterWritingDeltaAfterIssueingCredential + "\n" +  resultAfterCredentialIssueing.getObjectJson() + "\n");
        //




        // the prover stores the created credential in his wallet
        String credentialReferent = Anoncreds
                .proverStoreCredential(myWallet, null, proverCredReqResult.getCredentialRequestMetadataJson(),
                        createCredResult.getCredentialJson(), credentialResult.getCredDefJson(), revRegDef)
                .get();

        System.out.println("The credential has been stored under the uuid:\n" + credentialReferent + "\n");



        // the issuer revokes the credential and publishes the new delta on the ledger
        String credRevocId = createCredResult.getRevocId();
        System.out.println("The credential Revoc Id is:\n" + credRevocId + "\n");
        String newDeltaAfterRevocation = Anoncreds.issuerRevokeCredential(myWallet, blobReaderHandle, revRegDefId, credRevocId).get();
        request = Ledger.buildRevocRegEntryRequest(didTrustAnchor, revRegDefId, revDefType, newDeltaAfterRevocation).get();
        //System.out.println("Request to publish the new delta after Revocatin on the ledger:\n" + request + "\n");
        response = Ledger.signAndSubmitRequest(myPool, myWallet, didTrustAnchor, request).get();
        System.out.println("The issuer has revoked the credential and published the new accum delta on the ledger\n" + response + "\n");


        Thread.sleep(3*1000); // let the thread sleep, so we definetly get a timestamp which is bigger than the moment we revoked the credential




        // read accum from ledger
        long timestampAfterRevocation = getUnixTimeStamp();
        request = Ledger.buildGetRevocRegRequest(didTrustAnchor, revRegDefId, timestampAfterRevocation).get();
        response = Ledger.submitRequest(myPool, request).get();
        ParseRegistryResponseResult resultAfterRevocation = Ledger.parseGetRevocRegResponse(response).get();
        System.out.println("Accum Value at (after revocation): " + timestampAfterRevocation + "\n" +  resultAfterRevocation.getObjectJson() + "\n");
        //




        /*
         * The credential has been issued to the prover and he saved it.
         * Now we want to get a simple proof for licence_number.
         * We want the prover to reveal this attribute.
         *
         */

        // we create a proof request
        JSONObject proofRequest = new JSONObject();
        proofRequest.put("name", "proof_req");
        proofRequest.put("version", "0.1");
        proofRequest.put("nonce", "123432421212");
        JSONObject requested_attributes = new JSONObject();
        JSONObject attribute_info = new JSONObject();
        attribute_info.put("name", "licencenumber");
        JSONObject restrictions = new JSONObject();
        restrictions.put("issuer_did", didTrustAnchor); // the restriction is that the trust anchor issued the credential
        attribute_info.put("restrictions", restrictions);
        requested_attributes.put("attr1_referent", attribute_info);
        proofRequest.put("requested_attributes", requested_attributes);
        proofRequest.put("requested_predicates", new JSONObject());
        long timestamp = getUnixTimeStamp();
        // the credentials must not be revoked in the particular moment = timestamp
        //proofRequest.put("non_revoked", new JSONObject().put("from", timestamp).put("to", timestamp));

        System.out.println("Proof-Request has beed created\n" + proofRequest + "\n");



        // build requested credentials which are needed for the actual proof
        String credentials_for_proofRequest = Anoncreds
                .proverGetCredentialsForProofReq(myWallet, proofRequest.toString()).get();

        JSONObject requestedCredentials = new JSONObject();
        JSONObject reqAttributes = new JSONObject();
        long proverTimestamp = getUnixTimeStamp(); // this is the timestamp of the moment in which the proover creates the non revocation proof
        reqAttributes.put("attr1_referent", new JSONObject().put("timestamp", timestamp).put("cred_id", credentialReferent).put("revealed", true));
        requestedCredentials.put("self_attested_attributes", new JSONObject());
        requestedCredentials.put("requested_attributes", reqAttributes);
        requestedCredentials.put("requested_predicates", new JSONObject());

        // create schemas which participate in the proof
        JSONObject schemas = new JSONObject();
        schemas.put(schemaResult.getSchemaId(), new JSONObject(schemaResult.getSchemaJson()));

        // create creds which participate in the proof
        JSONObject creds = new JSONObject();
        creds.put(credentialResult.getCredDefId(), new JSONObject(credentialResult.getCredDefJson()));


        // create the revocation states which participate in the proof
        request = Ledger.buildGetRevocRegDeltaRequest(null, revRegDefId, timestamp, timestamp).get(); // read the delta for the interval, which was requested in the proof request
        response = Ledger.submitRequest(myPool, request).get();
        System.out.println("Read the delta from the ledger response:\n" + response + "\n");

        ParseRegistryResponseResult deltaResult = Ledger.parseGetRevocRegDeltaResponse(response).get();
        String delta = deltaResult.getObjectJson();
        System.out.println("revoc ID MAGICOOO "+ createCredResult.getRevocId() );
        // the prover creates the revocaton state in the proverTimestamp moment
        String revState = Anoncreds.createRevocationState(blobReaderHandle, storeRevocResult.getRevRegDefJson(), delta, proverTimestamp, createCredResult.getRevocId()).get();
        JSONObject revStates = new JSONObject();
        revStates.put(revRegDefId, new JSONObject().put(Long.toString(timestamp), new JSONObject(revState)));
        System.out.println("The revocation states have been created\n" + revStates);

        /*
         * The has created the revocation state at the particular moment which is defined by proverTimestamp.
         * He created the revocation state for the interval, which was given by the proof request
         */


        // prover create proof
        String proof = Anoncreds.proverCreateProof(myWallet, proofRequest.toString(), requestedCredentials.toString(),
                linkSecret, schemas.toString(), creds.toString(), revStates.toString()).get();



        System.out.println("The prover has created a proof for the given proof request\n" + proof + "\n");


        /*
         * The verifier verifies the credential. Note that the issuer has revoked the credential
         * before the prover created the proof. More precisely, the issuer revoked the credential
         * prior proverTimestamp and published the delta on the ledger.
         */


        // the prover creates his own revocation definitions.
        JSONObject revocRegDefs = new JSONObject();
        request = Ledger.buildGetRevocRegDefRequest(didTrustAnchor, revRegDefId).get();
        response = Ledger.signAndSubmitRequest(myPool, myWallet, didTrustAnchor, request).get();
        ParseResponseResult parseResult = Ledger.parseGetRevocRegDefResponse(response).get();
        String revRegDefReadFromLedgerByVerifier = parseResult.getObjectJson();
        revocRegDefs.put(revRegDefId, new JSONObject(revRegDefReadFromLedgerByVerifier));
        System.out.println("Prover has build his own Revocation Defs:\n" + revocRegDefs + "\n");

        // the prover creates his own revocation states. Herefor he uses the timestamps from the
        // original proofrequest
        JSONObject revocRegs = new JSONObject();
        long from = timestamp;
        long to = timestamp;

        request = Ledger.buildGetRevocRegDeltaRequest(didTrustAnchor, revRegDefId, from, to).get();
        response = Ledger.signAndSubmitRequest(myPool, myWallet, didTrustAnchor, request).get();
        System.out.println("Prover has read the revoc delta for interval from: " + from + "to: " + to + " response from ledger \n" + response + "\n");
        ParseRegistryResponseResult  parseRegRespResult = Ledger.parseGetRevocRegDeltaResponse(response).get();
        String proverReadDeltaFromLedger = parseRegRespResult.getObjectJson();
        System.out.println("Prover has read the revoc delta for interval from: " + from + "to: " + to + "\n" + proverReadDeltaFromLedger + "\n");

        /*
         * revoc delta for current timestamp
         */
        long time = getUnixTimeStamp();
        request = Ledger.buildGetRevocRegDeltaRequest(didTrustAnchor, revRegDefId, time, time).get();
        response = Ledger.signAndSubmitRequest(myPool, myWallet, didTrustAnchor, request).get();
        System.out.println("Prover has read the revoc delta for interval from: " + time + "to: " + time + " response from ledger \n" + response + "\n");
        parseRegRespResult = Ledger.parseGetRevocRegDeltaResponse(response).get();
        proverReadDeltaFromLedger = parseRegRespResult.getObjectJson();
        System.out.println("Prover has read the revoc delta for interval from: " + time + "to: " + time + "\n" + proverReadDeltaFromLedger + "\n");
        /*
         *
         */


        revocRegs.put(revRegDefId, new JSONObject().put(Long.toString(proverTimestamp), new JSONObject(proverReadDeltaFromLedger)));
        System.out.println("Prover has build his own Revocation States:\n" + revocRegDefs.toString(4) + "\n");


		/*
		request = Ledger.buildGetRevocRegRequest(didProver, revRegDefId, proverTimestamp).get();
		response = Ledger.submitRequest(myPool, request).get();
		System.out.println("Read RevocReg from Ledger response:\n" + response + "\n");


		request = Ledger.buildGetRevocRegRequest(didProver, revRegDefId, getUnixTimeStamp()).get();
		response = Ledger.submitRequest(myPool, request).get();
		System.out.println("Read RevocReg from Ledger response:\n" + response + "\n");
		*/

        System.out.println("printing all the way:"+
                "\n createdProof.request 1: "+proofRequest.toString(4)
                +"\n createdProof proof 2 :" + proof
                +"\n createdProof proof 3:" + schemas.toString(4)+
                "+\n created proof 4:"+ creds.toString(4)+
                "\n revocregdegs 5: "+ revocRegDefs.toString(4) +
                "\n revocReg 6:"+ revocRegs.toString(4));





        Boolean verifyResult = Anoncreds.verifierVerifyProof(proofRequest.toString(), proof, schemas.toString(),
                creds.toString(), revocRegDefs.toString(), revocRegs.toString()).get();
        System.out.println("The proof result ist: " + verifyResult);




        myPool.close();
        myWallet.close();




    }

    private static JSONObject encode(JSONObject attributesToIssue) {
        try {
            JSONObject result = new JSONObject();
            Iterator<String> keyIterator = attributesToIssue.keys();
            while (keyIterator.hasNext()) {
                String key = keyIterator.next();
                String rawValue = attributesToIssue.getString(key);
                String encValue = encStringAsInt(rawValue);
                result.put(key, new JSONObject().put("raw", rawValue).put("encoded", encValue));
            }

            return result;
        } catch (JSONException e) {
            return null;
        }
    }

    private static String encStringAsInt(String string) {
        try {
            Integer.parseInt(string);
            return string;
        } catch (Exception e) {
            BigInteger bigInt = new BigInteger(string.getBytes());
            return bigInt.toString();
        }
    }

    private static String[] getDefaultGenesisTxn(String poolIPAddress) {
        String[] s = new String[] { String.format(
                "{\"reqSignature\":{},\"txn\":{\"data\":{\"data\":{\"alias\":\"Node1\",\"blskey\":\"4N8aUNHSgjQVgkpm8nhNEfDf6txHznoYREg9kirmJrkivgL4oSEimFF6nsQ6M41QvhM2Z33nves5vfSn9n1UwNFJBYtWVnHYMATn76vLuL3zU88KyeAYcHfsih3He6UHcXDxcaecHVz6jhCYz1P2UZn2bDVruL5wXpehgBfBaLKm3Ba\",\"blskey_pop\":\"RahHYiCvoNCtPTrVtP7nMC5eTYrsUA8WjXbdhNc8debh1agE9bGiJxWBXYNFbnJXoXhWFMvyqhqhRoq737YQemH5ik9oL7R4NTTCz2LEZhkgLJzB3QRQqJyBNyv7acbdHrAT8nQ9UkLbaVL9NBpnWXBTw4LEMePaSHEw66RzPNdAX1\",\"client_ip\":\"%s\",\"client_port\":9702,\"node_ip\":\"%s\",\"node_port\":9701,\"services\":[\"VALIDATOR\"]},\"dest\":\"Gw6pDLhcBcoQesN72qfotTgFa7cbuqZpkX3Xo6pLhPhv\"},\"metadata\":{\"from\":\"Th7MpTaRZVRYnPiabds81Y\"},\"type\":\"0\"},\"txnMetadata\":{\"seqNo\":1,\"txnId\":\"fea82e10e894419fe2bea7d96296a6d46f50f93f9eeda954ec461b2ed2950b62\"},\"ver\":\"1\"}",
                poolIPAddress, poolIPAddress),
                String.format(
                        "{\"reqSignature\":{},\"txn\":{\"data\":{\"data\":{\"alias\":\"Node2\",\"blskey\":\"37rAPpXVoxzKhz7d9gkUe52XuXryuLXoM6P6LbWDB7LSbG62Lsb33sfG7zqS8TK1MXwuCHj1FKNzVpsnafmqLG1vXN88rt38mNFs9TENzm4QHdBzsvCuoBnPH7rpYYDo9DZNJePaDvRvqJKByCabubJz3XXKbEeshzpz4Ma5QYpJqjk\",\"blskey_pop\":\"Qr658mWZ2YC8JXGXwMDQTzuZCWF7NK9EwxphGmcBvCh6ybUuLxbG65nsX4JvD4SPNtkJ2w9ug1yLTj6fgmuDg41TgECXjLCij3RMsV8CwewBVgVN67wsA45DFWvqvLtu4rjNnE9JbdFTc1Z4WCPA3Xan44K1HoHAq9EVeaRYs8zoF5\",\"client_ip\":\"%s\",\"client_port\":9704,\"node_ip\":\"%s\",\"node_port\":9703,\"services\":[\"VALIDATOR\"]},\"dest\":\"8ECVSk179mjsjKRLWiQtssMLgp6EPhWXtaYyStWPSGAb\"},\"metadata\":{\"from\":\"EbP4aYNeTHL6q385GuVpRV\"},\"type\":\"0\"},\"txnMetadata\":{\"seqNo\":2,\"txnId\":\"1ac8aece2a18ced660fef8694b61aac3af08ba875ce3026a160acbc3a3af35fc\"},\"ver\":\"1\"}\n",
                        poolIPAddress, poolIPAddress),
                String.format(
                        "{\"reqSignature\":{},\"txn\":{\"data\":{\"data\":{\"alias\":\"Node3\",\"blskey\":\"3WFpdbg7C5cnLYZwFZevJqhubkFALBfCBBok15GdrKMUhUjGsk3jV6QKj6MZgEubF7oqCafxNdkm7eswgA4sdKTRc82tLGzZBd6vNqU8dupzup6uYUf32KTHTPQbuUM8Yk4QFXjEf2Usu2TJcNkdgpyeUSX42u5LqdDDpNSWUK5deC5\",\"blskey_pop\":\"QwDeb2CkNSx6r8QC8vGQK3GRv7Yndn84TGNijX8YXHPiagXajyfTjoR87rXUu4G4QLk2cF8NNyqWiYMus1623dELWwx57rLCFqGh7N4ZRbGDRP4fnVcaKg1BcUxQ866Ven4gw8y4N56S5HzxXNBZtLYmhGHvDtk6PFkFwCvxYrNYjh\",\"client_ip\":\"%s\",\"client_port\":9706,\"node_ip\":\"%s\",\"node_port\":9705,\"services\":[\"VALIDATOR\"]},\"dest\":\"DKVxG2fXXTU8yT5N7hGEbXB3dfdAnYv1JczDUHpmDxya\"},\"metadata\":{\"from\":\"4cU41vWW82ArfxJxHkzXPG\"},\"type\":\"0\"},\"txnMetadata\":{\"seqNo\":3,\"txnId\":\"7e9f355dffa78ed24668f0e0e369fd8c224076571c51e2ea8be5f26479edebe4\"},\"ver\":\"1\"}\n",
                        poolIPAddress, poolIPAddress),
                String.format(
                        "{\"reqSignature\":{},\"txn\":{\"data\":{\"data\":{\"alias\":\"Node4\",\"blskey\":\"2zN3bHM1m4rLz54MJHYSwvqzPchYp8jkHswveCLAEJVcX6Mm1wHQD1SkPYMzUDTZvWvhuE6VNAkK3KxVeEmsanSmvjVkReDeBEMxeDaayjcZjFGPydyey1qxBHmTvAnBKoPydvuTAqx5f7YNNRAdeLmUi99gERUU7TD8KfAa6MpQ9bw\",\"blskey_pop\":\"RPLagxaR5xdimFzwmzYnz4ZhWtYQEj8iR5ZU53T2gitPCyCHQneUn2Huc4oeLd2B2HzkGnjAff4hWTJT6C7qHYB1Mv2wU5iHHGFWkhnTX9WsEAbunJCV2qcaXScKj4tTfvdDKfLiVuU2av6hbsMztirRze7LvYBkRHV3tGwyCptsrP\",\"client_ip\":\"%s\",\"client_port\":9708,\"node_ip\":\"%s\",\"node_port\":9707,\"services\":[\"VALIDATOR\"]},\"dest\":\"4PS3EDQ3dW1tci1Bp6543CfuuebjFrg36kLAUcskGfaA\"},\"metadata\":{\"from\":\"TWwCRQRZ2ZHMJFn9TzLp7W\"},\"type\":\"0\"},\"txnMetadata\":{\"seqNo\":4,\"txnId\":\"aa5e817d7cc626170eca175822029339a444eb0ee8f0bd20d3b0b76e566fb008\"},\"ver\":\"1\"}",
                        poolIPAddress, poolIPAddress) };
        return s;
    }

    private static void writeGenesisTransactions(String[] genesisContent, String genesisFileName) throws IOException {
        File genesisFile = new File(indyClientPath + "/" + genesisFileName);
        FileWriter fw = new FileWriter(genesisFile);
        for (String s : genesisContent) {
            fw.write(s);
            fw.write("\n");
        }
        fw.flush();
        fw.close();

    }


    public static long getUnixTimeStamp() {
        return Instant.now().getEpochSecond();
    }

}