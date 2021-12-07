package OldTests;

import IndyLibraries.Endorser;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.anoncreds.Anoncreds;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults;
import org.hyperledger.indy.sdk.anoncreds.CredentialsSearchForProofReq;
import org.hyperledger.indy.sdk.blob_storage.BlobStorageReader;
import org.hyperledger.indy.sdk.blob_storage.BlobStorageWriter;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

import static org.hyperledger.indy.sdk.anoncreds.Anoncreds.*;

public class DEMO3 {
    public static void main (String args[]){
        System.out.println("Anoncreds Revocation sample -> started");

        String issuerDid = "NcYxiDXkpYi6ov5FcYDi1e";
        String proverDid = "VsKV7grR1BUE29mG2Fm2kX";

        // Set protocol version 2 to work with Indy Node 1.4
        try {
            Pool.setProtocolVersion(2).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }

        //1. Create and Open Pool
        //String poolName = IndyLibraries.PoolUtils.createPoolLedgerConfig();
        Pool pool = null;
        try {
            pool = Pool.openPoolLedger("INDYSCANPOOL", "{}").get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }

        //2. Issuer Create and Open Wallet
        String issuerWalletConfig = new JSONObject().put("id", "issuerWallet").toString();
        String issuerWalletCredentials = new JSONObject().put("key", "issuer_wallet_key").toString();
        try {
            Wallet.createWallet(issuerWalletConfig, issuerWalletCredentials).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        Wallet issuerWallet = null;
        try {
            issuerWallet = Wallet.openWallet(issuerWalletConfig, issuerWalletCredentials).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }

        //3. Prover Create and Open Wallet
        String proverWalletConfig = new JSONObject().put("id", "trusteeWallet").toString();
        String proverWalletCredentials = new JSONObject().put("key", "prover_wallet_key").toString();
        try {
            Wallet.createWallet(proverWalletConfig, proverWalletCredentials).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        Wallet proverWallet = null;
        try {
            proverWallet = Wallet.openWallet(proverWalletConfig, proverWalletCredentials).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }

        //4. Issuer Creates Credential Schema
        String schemaName = "gvt";
        String schemaVersion = "1.0";
        String schemaAttributes = new JSONArray().put("name").put("age").put("sex").put("height").toString();
        AnoncredsResults.IssuerCreateSchemaResult createSchemaResult =
                null;
        try {
            createSchemaResult = issuerCreateSchema(issuerDid, schemaName, schemaVersion, schemaAttributes).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        String schemaId = createSchemaResult.getSchemaId();
        String schemaJson = createSchemaResult.getSchemaJson();

        //5. Issuer create Credential Definition
        String credDefTag = "Tag1";
        String credDefConfigJson = new JSONObject().put("support_revocation", true).toString();
        AnoncredsResults.IssuerCreateAndStoreCredentialDefResult createCredDefResult =
                null;
        try {
            createCredDefResult = issuerCreateAndStoreCredentialDef(issuerWallet, issuerDid, schemaJson, credDefTag, null, credDefConfigJson).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        String credDefId = createCredDefResult.getCredDefId();
        String credDefJson = createCredDefResult.getCredDefJson();

        //6. Issuer create Revocation Registry
        String revRegDefConfig = new JSONObject()
                .put("issuance_type", "ISSUANCE_ON_DEMAND")
                .put("max_cred_num", 5)
                .toString();
        String tailsWriterConfig = new JSONObject()
                .put("base_dir", Endorser.getIndyHomePath("tails").replace('\\', '/'))
                .put("uri_pattern", "")
                .toString();

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

        String revRegDefTag = "Tag2";
        AnoncredsResults.IssuerCreateAndStoreRevocRegResult createRevRegResult =
                null;
        try {
            System.out.println();
            createRevRegResult = issuerCreateAndStoreRevocReg(issuerWallet, issuerDid, null, revRegDefTag, credDefId, revRegDefConfig, tailsWriter).get();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        String revRegId = createRevRegResult.getRevRegId();
        String revRegDefJson = createRevRegResult.getRevRegDefJson();

        //7. Prover create Master Secret
        String masterSecretId = null;
        try {
            masterSecretId = proverCreateMasterSecret(proverWallet, null).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }

        //8. Issuer Creates Credential Offer
        String credOffer = null;
        try {
            credOffer = issuerCreateCredentialOffer(issuerWallet, credDefId).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }

        //9. Prover Creates Credential Request
        AnoncredsResults.ProverCreateCredentialRequestResult createCredReqResult =
                null;
        try {
            createCredReqResult = proverCreateCredentialReq(proverWallet, proverDid, credOffer, credDefJson, masterSecretId).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        String credReqJson = createCredReqResult.getCredentialRequestJson();
        String credReqMetadataJson = createCredReqResult.getCredentialRequestMetadataJson();

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

        //11. Issuer create Credential
        //    note that encoding is not standardized by Indy except that 32-bit integers are encoded as themselves. IS-786
        String credValuesJson = new JSONObject()
                .put("sex", new JSONObject().put("raw", "male").put("encoded", "594465709955896723921094925839488742869205008160769251991705001"))
                .put("name", new JSONObject().put("raw", "Alex").put("encoded", "1139481716457488690172217916278103335"))
                .put("height", new JSONObject().put("raw", "175").put("encoded", "175"))
                .put("age", new JSONObject().put("raw", "28").put("encoded", "28"))
                .toString();

        AnoncredsResults.IssuerCreateCredentialResult createCredentialResult =
                null;
        try {
            createCredentialResult = Anoncreds.issuerCreateCredential(issuerWallet, credOffer, credReqJson, credValuesJson, revRegId, blobStorageReaderHandle).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        String credentialJson = createCredentialResult.getCredentialJson();
        String revRegDeltaJson = createCredentialResult.getRevocRegDeltaJson();
        String credRevId = createCredentialResult.getRevocId();

        //12. Prover Stores Credential
        try {
            Anoncreds.proverStoreCredential(proverWallet, null, credReqMetadataJson, credentialJson, credDefJson, revRegDefJson).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }

        //13. Prover Gets Credentials for Proof Request
        long timestamp = System.currentTimeMillis() / 1000;
        String nonce = null;
        try {
            nonce = generateNonce().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        String proofRequestJson = new JSONObject()
                .put("nonce", nonce)
                .put("name", "proof_req_1")
                .put("version", "0.1")
                .put("requested_attributes", new JSONObject()
                        .put("attr1_referent", new JSONObject().put("name", "name"))
                )
                .put("requested_predicates", new JSONObject()
                        .put("predicate1_referent", new JSONObject()
                                .put("name", "age")
                                .put("p_type", ">=")
                                .put("p_value", 18)
                        )
                )
                .put("non_revoked", new JSONObject()
                        .put("to", timestamp)
                )
                .toString();

        CredentialsSearchForProofReq credentialsSearch = null;
        try {
            credentialsSearch = CredentialsSearchForProofReq.open(proverWallet, proofRequestJson, null).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }

        JSONArray credentialsForAttribute1 = null;
        try {
            credentialsForAttribute1 = new JSONArray(credentialsSearch.fetchNextCredentials("attr1_referent", 100).get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        String credIdForAttr1 = credentialsForAttribute1.getJSONObject(0).getJSONObject("cred_info").getString("referent");

        JSONArray credentialsForAttribute2 = null;
        try {
            credentialsForAttribute2 = new JSONArray(credentialsSearch.fetchNextCredentials("predicate1_referent", 100).get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        String credIdForPred1 = credentialsForAttribute2.getJSONObject(0).getJSONObject("cred_info").getString("referent");

        try {
            credentialsSearch.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //14. Prover create RevocationState
        String revStateJson = null;
        try {
            revStateJson = Anoncreds.createRevocationState(blobStorageReaderHandle, revRegDefJson, revRegDeltaJson, timestamp, credRevId).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }

        //15. Prover Creates Proof
        String requestedCredentialsJson = new JSONObject()
                .put("self_attested_attributes", new JSONObject())
                .put("requested_attributes", new JSONObject()
                        .put("attr1_referent", new JSONObject()
                                .put("cred_id", credIdForAttr1)
                                .put("revealed", true)
                                .put("timestamp", timestamp)
                        )
                )
                .put("requested_predicates", new JSONObject()
                        .put("predicate1_referent", new JSONObject()
                                .put("cred_id", credIdForPred1)
                                .put("timestamp", timestamp)
                        )
                )
                .toString();

        String schemas = new JSONObject().put(schemaId, new JSONObject(schemaJson)).toString();
        String credentialDefs = new JSONObject().put(credDefId, new JSONObject(credDefJson)).toString();
        String revStates = new JSONObject().put(revRegId, new JSONObject().put("" + timestamp, new JSONObject(revStateJson))).toString();

        String proofJson = null;
        //necessario un revocation registry Delta:
        // 	 Parse a GET_REVOC_REG_DELTA response to get
        // 	 Revocation Registry Delta in the
        // 	 format compatible with Anoncreds API.
        try {
            proofJson = Anoncreds.proverCreateProof(proverWallet, proofRequestJson, requestedCredentialsJson, masterSecretId, schemas,
                    credentialDefs, revStates).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        JSONObject proof = new JSONObject(proofJson);

        //16. Verifier verify Proof
        JSONObject revealedAttr1 = proof.getJSONObject("requested_proof").getJSONObject("revealed_attrs").getJSONObject("attr1_referent");

        String revRegDefs = new JSONObject().put(revRegId, new JSONObject(revRegDefJson)).toString();
        String revRegs = new JSONObject().put(revRegId, new JSONObject().put("" + timestamp, new JSONObject(revRegDeltaJson))).toString();
        try {
            credOffer = issuerCreateCredentialOffer(issuerWallet, credDefId).get();
            createCredReqResult = proverCreateCredentialReq(proverWallet, proverDid, credOffer, credDefJson, masterSecretId).get();
             credValuesJson = new JSONObject()
                    .put("sex", new JSONObject().put("raw", "male").put("encoded", "594465709955896723921094925839488742869205008160769251991705001"))
                    .put("name", new JSONObject().put("raw", "Alex").put("encoded", "1139481716457488690172217916278103335"))
                    .put("height", new JSONObject().put("raw", "175").put("encoded", "175"))
                    .put("age", new JSONObject().put("raw", "98").put("encoded", "98"))
                    .toString();

             Anoncreds.createRevocationState(blobStorageReaderHandle, revRegDeltaJson,revRegDeltaJson,timestamp
                     ,credRevId).get();


        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }

        System.out.println("revRegDefs"+ revRegDefs);
        System.out.println("revDegs"+ revRegs);
        try {
            boolean valid = Anoncreds.verifierVerifyProof(proofRequestJson, proofJson, schemas, credentialDefs, revRegDefs, revRegs).get();
            System.out.println("verify after revocation: "+ valid+"bye bye");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }

        //17. Close and Delete issuer wallet
        try {
            issuerWallet.closeWallet().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        try {
            Wallet.deleteWallet(issuerWalletConfig, issuerWalletCredentials).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }

        //18. Close and Delete prover wallet
        try {
            proverWallet.closeWallet().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        try {
            Wallet.deleteWallet(proverWalletConfig, proverWalletCredentials).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }

        //19. Close pool
        try {
            pool.closePoolLedger().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        try {
            issuerWallet.closeWallet().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        try {
            Wallet.deleteWallet(issuerWalletConfig, issuerWalletCredentials).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }

        //18. Close and Delete prover wallet
        try {proverWallet.closeWallet().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        try {
            Wallet.deleteWallet(proverWalletConfig, proverWalletCredentials).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }


        //20. Delete Pool ledger config

        System.out.println("Anoncreds Revocation sample -> completed");
    }
}
