package IndyLibraries;

import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.anoncreds.Anoncreds;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults;
import org.hyperledger.indy.sdk.anoncreds.CredentialsSearchForProofReq;
import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.did.DidResults;
import org.hyperledger.indy.sdk.ledger.Ledger;
import org.hyperledger.indy.sdk.ledger.LedgerResults;
import org.hyperledger.indy.sdk.metrics.Metrics;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.hyperledger.indy.sdk.wallet.WalletItemAlreadyExistsException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import static org.hyperledger.indy.sdk.ledger.Ledger.*;
//Java class that encapsulates an Agent object with all the capabilities of an Agent without an Identity onto the Ledger.
public class Agent {
    protected String agentName;
    public Pool poolConnection;
    protected JSONUserCredentialStorage agentsFile;

    public Wallet mainWallet;//WalletHandle

    public DIDStructure mainDID;//(DIDValue,DIDVerkey)
    protected HashMap<String, DIDStructure> DIDCollection;//DIDValue->(DIDValue,DIDVerkey)
    //a local cache of schema and cred defs
    protected HashMap<String, SchemaStructure> SchemaCollection;//SchemaID->(SchemaID,SchemaJson)
    protected HashMap<String, CredDefStructure> CredDefsCollection;//CredDefID->(CredDefID,CredDefJson)
    protected String nonce;

    protected String masterSecret;

    public Agent(Pool poolConnection, String agentName, JSONUserCredentialStorage agentsFile) {
        this.poolConnection = poolConnection;
        this.agentName = agentName;
        this.DIDCollection = new HashMap<>();
        this.CredDefsCollection = new HashMap<>();
        this.SchemaCollection = new HashMap<>();
        this.agentsFile = agentsFile;
        String masterSecret, DID, VerKey;
        masterSecret = DID = VerKey = null;
        if ((masterSecret = agentsFile.getMasterSecret(agentName)) != null) {
            this.masterSecret = masterSecret;
        }
        if ((DID = agentsFile.getAgentDID(agentName)) != null) {
            this.mainDID = new DIDStructure(DID, null);
            this.DIDCollection.put(agentName, this.mainDID);
        }
        if ((VerKey = agentsFile.getAgentVerKey(agentName)) != null) {
            //create a structure for the current Agent in the backup agents ifle
            if (this.mainDID != null) {
                //cant have a verkey without an associated DID
                this.mainDID.setVerKey(VerKey);
                this.masterSecret = masterSecret;
            }
        } else {
            this.agentsFile.insertAgentName(agentName);
            try {
                this.agentsFile.makeBackup();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Agent(Pool pool, String agentName, String DID, String Verkey) throws IllegalArgumentException {
        //crea nuovo utente con VerKey e DID specifici, solo se non esiste già tra gli IndyLibraries.Agent Salvati
        this.poolConnection = pool;
        this.agentName = agentName;
        this.mainDID = new DIDStructure(DID, Verkey);
        this.DIDCollection = new HashMap<>();
        this.CredDefsCollection = new HashMap<>();
        this.SchemaCollection = new HashMap<>();
        this.masterSecret = null;
    }


    //effects:setMainWallet to the wallet identified by walletName if none and add
    //this wallet to the list of wallets
    public Wallet OpenWallet(String walletName, String walletKeyPassword) {
        Wallet walletHandle = null;
        String issuerWalletConfig = new JSONObject().put("id", walletName).toString(4);
        String issuerWalletCredentials = new JSONObject().put("key", walletKeyPassword).toString(4);
        try {
            walletHandle = Wallet.openWallet(issuerWalletConfig, issuerWalletCredentials).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        if (walletHandle != null) {
            if (mainWallet == null)
                mainWallet = walletHandle;
        }
        return walletHandle;
    }

    public Boolean CreateWallet(String walletName, String walletKeyPassword) {
        try {

            Wallet.createWallet(new JSONObject().put("id", walletName).toString(4),
                    new JSONObject().put("key", walletKeyPassword).toString(4)).get();
        } catch (InterruptedException e) {
            System.out.println("CreateWallet, CxecutionException exception, probably wallet with specified name already exists");
            return false;
        } catch (ExecutionException e) {
            return false;
        } catch (IndyException e) {
            return false;
        }
        return true;
    }

    //create new DID with Default Seed, return DIDValue, add this (DIDValue,Verkey) to the DID collection
    //and if mainDID is null then sets it as mainDID
    public String createDID() {
        String DID = null;
        String DIDVerkey = null;
        DIDStructure didStructure;
        DidResults.CreateAndStoreMyDidResult AgentDIDResult =
                null;
        try {

            AgentDIDResult = Did.createAndStoreMyDid(mainWallet, IndyJsonStringBuilder.getEmptyJson()).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        this.agentsFile.insertAgentDID(this.agentName, AgentDIDResult.getDid());
        this.agentsFile.insertAgentVerKey(this.agentName, AgentDIDResult.getVerkey());
        try {
            this.agentsFile.makeBackup();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return addDIDToCollectionSetMainDid(AgentDIDResult);
    }
    public String collectMetrics(){
        try {
            //return libindy metrics for current process
            return new JSONObject(Metrics.collectMetrics().get()).toString(4);
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;

    }
    //create DID based on Seed
    public String createDID(String didSeed) {
        String DID = null;
        String DIDVerkey = null;
        DIDStructure didStructure;
        DidResults.CreateAndStoreMyDidResult AgentDIDResult =
                null;
        try {
            AgentDIDResult = Did.createAndStoreMyDid(mainWallet, IndyJsonStringBuilder.createDIDString(null, didSeed
                    , null, null, null)).get();
            this.agentsFile.insertAgentDID(this.agentName, AgentDIDResult.getDid());
            this.agentsFile.insertAgentVerKey(this.agentName, AgentDIDResult.getVerkey());
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return addDIDToCollectionSetMainDid(AgentDIDResult);

    }

    private String addDIDToCollectionSetMainDid(DidResults.CreateAndStoreMyDidResult agentDIDResult) {
        String DID;
        String DIDVerkey;
        DIDStructure didStructure;
        if (agentDIDResult == null)
            return null;
        DID = agentDIDResult.getDid();
        DIDVerkey = agentDIDResult.getVerkey();
        didStructure = new DIDStructure(DID, DIDVerkey);
        if (mainDID == null) {
            mainDID = didStructure;
        }
        DIDCollection.put("DID", didStructure);
        return DID;
    }

    //create DID based on value and/or seedValue and specific characteristics
    public String createDID(String didValue, String seedValue, String cryptotypeValue,
                            Boolean cidValue, String methodnameValue) {
        String DID = null;
        String DIDVerkey = null;
        DIDStructure didStructure;
        DidResults.CreateAndStoreMyDidResult AgentDIDResult =
                null;
        try {
            AgentDIDResult = Did.createAndStoreMyDid(mainWallet, IndyJsonStringBuilder.
                    createDIDString(didValue, seedValue, cryptotypeValue, cidValue, methodnameValue)).get();
            this.agentsFile.insertAgentDID(this.agentName, AgentDIDResult.getDid());
            this.agentsFile.insertAgentVerKey(this.agentName, AgentDIDResult.getVerkey());
            this.agentsFile.makeBackup();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return addDIDToCollectionSetMainDid(AgentDIDResult);

    }
    public String getStoredDIDandVerkey(String didValue) {
        String DID = null;
        String DIDVerkey = null;
        DIDStructure didStructure;
        DidResults.CreateAndStoreMyDidResult AgentDIDResult =
                null;
        JSONObject retrievedDID = null;
        try {
             retrievedDID = new JSONObject (Did.getDidWithMeta(mainWallet,didValue).get());
            this.agentsFile.insertAgentDID(this.agentName, retrievedDID.getString("did"));
            this.agentsFile.insertAgentVerKey(this.agentName, retrievedDID.getString("verkey"));
            this.agentsFile.makeBackup();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return addDIDToCollectionSetMainDid(retrievedDID.getString("did"),retrievedDID.getString("verkey"));


    }



    //get SchemaJson from ledger and add the IndyLibraries.SchemaStructure to the IndyLibraries.Agent Collection
    public String getNYMFromLedger(String destDID) {
        String responseNym = null;
        try {
            String getNymRequest = Ledger.buildGetNymRequest(this.mainDID.didName, destDID).get();
            String s = Ledger.submitRequest(this.poolConnection, getNymRequest).get();
            responseNym = parseGetNymResponse(s).get();
            return new JSONObject(s).getJSONObject("result").toString(4);
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

    //get SchemaJson from ledger and add the IndyLibraries.SchemaStructure to the IndyLibraries.Agent Collection
    public String getATTRIBFromLedger(String destDID, String optRaw, String optHash, String optencryptedval) {
        String responseNym = null;
        if(optRaw == null && optencryptedval == null && optHash== null)
            return null;
        try {
            String getAttribRequest = Ledger.buildGetAttribRequest(this.mainDID.didName, destDID,
                    optRaw, optHash, optencryptedval).get();
            String s = Ledger.submitRequest(this.poolConnection, getAttribRequest).get();
            return new JSONObject(s).toString(4);
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

    public String getATTRIBFromLedger(String theirDID,String justRaw) {
        String responseNym = null;
        try {

            String getAttribRequest = Ledger.buildGetAttribRequest(this.mainDID.didName, theirDID,
                    justRaw, null, null).get();
            String s = Ledger.signAndSubmitRequest(this.poolConnection,this.mainWallet,this.mainDID.didName, getAttribRequest).get();
            return new JSONObject(s).toString(4);
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


    //get SchemaJson from ledger and add the IndyLibraries.SchemaStructure to the IndyLibraries.Agent Collection
    public String getSchemaFromLedger(String SchemaID) {
        String schemaIDOnLedger;
        String schemaJson;
        //necessario parsare lo schema ottenuto in uno frienldy con AnonCreds API e issuer credential
        LedgerResults.ParseResponseResult responseScheme = null;
        try {
            //System.out.println("id schema che cerco sul ledger"+SchemaID);
            String getSchemaRequest = Ledger.buildGetSchemaRequest(this.mainDID.didName,
                    SchemaID).get();
            String s = Ledger.submitRequest(this.poolConnection, getSchemaRequest).get();
            responseScheme = parseGetSchemaResponse(s).get();
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
        if (responseScheme != null) {
            schemaJson = responseScheme.getObjectJson();
            schemaIDOnLedger = responseScheme.getId();
            JSONArray jsonArray = new JSONObject(schemaJson).getJSONArray("attrNames");
            String nameOnLedger = new JSONObject(schemaJson).getString("name");
            String versionOnLedger = new JSONObject(schemaJson).getString("version");

            this.SchemaCollection.put(schemaIDOnLedger, new SchemaStructure(schemaIDOnLedger, nameOnLedger, schemaJson,
                    (String[]) jsonArray.toList().toArray(String[]::new), versionOnLedger));
            return schemaJson;
        }
        return null;
    }

    //get CredDef from ledger and add the IndyLibraries.SchemaStructure to the IndyLibraries.Agent Collection
    public String getCredentialDefinitionFromLedger(String credDefID) {
        String credDefIdOnLedger;
        String credDefJson;
        //necessario parsare lo schema ottenuto in uno frienldy con AnonCreds API e issuer credential
        LedgerResults.ParseResponseResult responseCredDef = null;
        try {
            //System.out.println("id schema che cerco sul ledger"+SchemaID);
            String getCredDefRequest = Ledger.buildGetCredDefRequest(this.mainDID.didName,
                    credDefID).get();
            String s = Ledger.submitRequest(this.poolConnection, getCredDefRequest).get();
            responseCredDef = parseGetSchemaResponse(s).get();
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
        if (responseCredDef != null) {
            credDefJson = responseCredDef.getObjectJson();
            credDefIdOnLedger = responseCredDef.getId();
            JSONArray jsonArray = new JSONObject(credDefJson).getJSONArray("attrNames");
            this.CredDefsCollection.put(credDefIdOnLedger, new CredDefStructure(credDefID.toString(), credDefJson.toString()));
            return credDefJson.toString();
        }
        return null;
    }


    //set this IndyLibraries.Agent mastersecret
    public String createMasterSecret() {
        try {
            this.masterSecret = Anoncreds.proverCreateMasterSecret(this.mainWallet, null).get();
            this.agentsFile.insertMasterSecret(this.agentName,this.masterSecret);
            this.agentsFile.makeBackup();
            return this.masterSecret;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String createMasterSecret(String masterSecretId) {
        try {
            this.masterSecret = Anoncreds.proverCreateMasterSecret(this.mainWallet, masterSecretId).get();
            this.agentsFile.insertMasterSecret(this.agentName,this.masterSecret);
            this.agentsFile.makeBackup();
            return this.masterSecret;

        } catch (InterruptedException e) {
        } catch (ExecutionException e) {
            this.masterSecret = masterSecretId;
            System.out.println("Thia master secret already exists " + this.masterSecret);
        } catch (IndyException e) {

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //get credential request structure (credreqJson,credredmetadata) for issuer
    //inside the credential request prover will put his master secret key in it, the master secret will make the credential
    //belong to the prover, any other Agent with the credential and without the master secret will not be able to create a
    //valid proof with it.
    public CredRequestStructure returnproverCreateCredentialRequest(CredOfferStructure credOffer) {
        AnoncredsResults.ProverCreateCredentialRequestResult createCredReqResult =
                null;
        String credReqJson, credReqMetadataJson;
        if (credOffer.credDef.credDefJson == null) {
            //credDef json is needed
            try {
                String req = buildGetCredDefRequest(this.mainDID.didName, credOffer.credDef.credDefId).get();
                String objJson = parseGetCredDefResponse(submitRequest(poolConnection, req).get()).get().getObjectJson();
                credOffer.credDef.credDefJson = objJson;
            } catch (IndyException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        try {
            if (this.masterSecret == null) {//creates a mastersecret for this Agent if there is not an existing already
                ////System.out.println creating master secret
                createMasterSecret();
                System.out.println("masterSecretAtCreation" + this.masterSecret);
            }
            createCredReqResult = Anoncreds.proverCreateCredentialReq(this.mainWallet, this.mainDID.didName, credOffer.credOffer, credOffer.credDef.credDefJson, this.masterSecret).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        if (createCredReqResult != null) {
            credReqJson = createCredReqResult.getCredentialRequestJson();
            credReqMetadataJson = createCredReqResult.getCredentialRequestMetadataJson();
            return new CredRequestStructure(credReqJson, credReqMetadataJson);
        }
        return null;
    }
    //get credential request structure (credreqJson,credredmetadata) for issuer by future prover,
    //in this case we use a DID for request!
    public CredRequestStructure returnproverCreateCredentialRequest(CredOfferStructure credOffer,String myDID2DID) {
        AnoncredsResults.ProverCreateCredentialRequestResult createCredReqResult =
                null;
        String credReqJson, credReqMetadataJson;
        if (credOffer.credDef.credDefJson == null) {
            //credDef json is needed
            try {
                String req = buildGetCredDefRequest(myDID2DID, credOffer.credDef.credDefId).get();
                String objJson = parseGetCredDefResponse(submitRequest(poolConnection, req).get()).get().getObjectJson();
                credOffer.credDef.credDefJson = objJson;
            } catch (IndyException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        try {
            if (this.masterSecret == null) {
                ////System.out.println creating master secret
                createMasterSecret();
                System.out.println("masterSecretAtCreation" + this.masterSecret);
            }
            createCredReqResult = Anoncreds.proverCreateCredentialReq(this.mainWallet, myDID2DID, credOffer.credOffer, credOffer.credDef.credDefJson, this.masterSecret).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        if (createCredReqResult != null) {
            credReqJson = createCredReqResult.getCredentialRequestJson();
            credReqMetadataJson = createCredReqResult.getCredentialRequestMetadataJson();
            return new CredRequestStructure(credReqJson, credReqMetadataJson);
        }
        return null;
    }


    //ccredDefJson is given by the Issuer to the Prover,alongside  credential
    //credentialID is an unique identifier of the credential, if there is a duplicate it will result in
    //an ItemAlreadyExists exception, easist thing to do is to keep it as null
    public String storeCredentialInWallet(String credentialID,String credDefId, String credReqMetadataJson, String credential,
                                          String credDefJson, String revRegDefID) {
        CredDefStructure credDefStructure = this.CredDefsCollection.get("credId");
        String credentialWalletReferent = null;
        String revRegDefJson = null;
        if (credDefStructure == null) {
            credDefStructure = new CredDefStructure(credDefId, credDefJson);
        }
        if (revRegDefID != null) {//we need to retrieve the revocation registry definition for this credential
            revRegDefJson = getRevocationDefinition(revRegDefID);
        }
        try {
            credentialWalletReferent = Anoncreds.proverStoreCredential(this.mainWallet, credentialID, credReqMetadataJson, credential,
                    credDefJson, revRegDefJson).get();
            System.out.println("stored credential"+credentialWalletReferent);
            CredDefsCollection.put(credDefId, credDefStructure);
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
        return credentialWalletReferent;
    }

    public ProofCreationExtra returnProverSearchAttrAndCreateProofSimple(String proofRequestJson,
                                                                         String[] selfAttestedAttr,Long time) {
        //NECESSARIO CREARE UN'ALGORITMO CHE PER OGNI CREDENZIALE DEL CARO AGENT, CONTROLLI CHE CORRISPONDA AD
        //UNA REFERENCE CHE HA LUI
        JSONArray jsonArray;
        String credentialIdForAttribute, tmpcredId, tmpSchemaId;
        ArrayList<String> credentialID = new ArrayList<>();
        ArrayList<String> selfAttestedList = new ArrayList<>();
        ArrayList<String> toReferenceList = new ArrayList<>();
        ArrayList<String> requestedRevealed = new ArrayList<>();
        ArrayList<String> credDefIDs = new ArrayList<>();
        ArrayList<String> SchemaIDs = new ArrayList<>();

        try {
            System.out.println("pre proof requ pre fertch " + proofRequestJson);
            CredentialsSearchForProofReq credentialsSearch = CredentialsSearchForProofReq.open(this.mainWallet,
                    proofRequestJson, null).get();
            JSONObject jsonObject = new JSONObject(proofRequestJson).getJSONObject("requested_attributes");
            for (String attrRef : jsonObject.keySet()) {
                jsonArray = new JSONArray(credentialsSearch.fetchNextCredentials(attrRef, 100).get());
                String credId;
                if (jsonArray.length() > 0) {
                    System.out.println("Wallet Stored Credential: " + jsonArray.getJSONObject(0).toString(4));
                    tmpSchemaId = jsonArray.getJSONObject(0).getJSONObject("cred_info").getString("schema_id");
                    tmpcredId = jsonArray.getJSONObject(0).getJSONObject("cred_info").getString("cred_def_id");
                    credentialIdForAttribute = jsonArray.getJSONObject(0).getJSONObject("cred_info").getString("referent");
                    SchemaIDs.add(tmpSchemaId);

                    credDefIDs.add(tmpcredId);
                    //System.out.println("aa\naa"+jsonArray.getJSONObject(0).toString(4)+"aa");
                    credentialID.add(credentialIdForAttribute);
                    requestedRevealed.add("true");
                    toReferenceList.add(attrRef);

                } else {
                    selfAttestedList.add(attrRef);
                }
            }
            credentialsSearch.close();
            String requestedCredentialsJson;
            //NOTE: time must be null since we only create a proof for a non revocable credentials!
            if (selfAttestedAttr != null) {
                requestedCredentialsJson = IndyJsonStringBuilder.getProofBody((String[]) (selfAttestedList.toArray(String[]::new)),
                        selfAttestedAttr, (String[]) (toReferenceList.toArray(String[]::new)), (String[]) (credentialID.toArray(String[]::new)),
                        new HashMap<>(), null, null, null);
            } else {
                requestedCredentialsJson = IndyJsonStringBuilder.getProofBody(null,
                        null, (String[]) (toReferenceList.toArray(String[]::new)), (String[]) (credentialID.toArray(String[]::new)),
                        new HashMap<>(), null, null, null);

            }
            //prendere dal json l'id dello schema e della creddef
            String schemas;
            jsonObject = new JSONObject();
            for (String schemaId : (String[]) (new HashSet(SchemaIDs).toArray(String[]::new))) {
                //necessario trovare la credential definition corrispondente allo schema
                //the credential definition must match the credential schema
                //System.out.println("schemaIDReqested "+schemaId +"\n ");
                jsonObject.put(schemaId, new JSONObject(getSchemaFromLedger(schemaId))).toString(4);
            }
            schemas = jsonObject.toString(4);
            jsonObject = new JSONObject();
            String credentialDefs;
                for (String credDefId : (String[]) (new HashSet(credDefIDs)).toArray(String[]::new)) {
                //necessario trovare la credential definition corrispondente allo schema
                //the credential definition must match the credential schema
                //System.out.println("credDefIdRqeuqested "+credDefId+"\n  w");

                String req = Ledger.buildGetCredDefRequest(this.mainDID.didName, credDefId).get();
                String ris = Ledger.submitRequest(this.poolConnection, req).get();
                LedgerResults.ParseResponseResult responseResult =
                        Ledger.parseGetCredDefResponse(ris).get();
                jsonObject.put(credDefId, new JSONObject(responseResult.getObjectJson())).toString(4);
            }
            credentialDefs = jsonObject.toString(4);
            String revocStates = new JSONObject().toString(4);
            String proofJson = "";
            if (this.masterSecret == null)
                this.createMasterSecret();
            System.out.println("masterSecret" + this.masterSecret);
            //System.out.println("inizio\n1"+proofRequestJson+"\n2"+requestedCredentialsJson+"\n3"+ this.masterSecret+"\n4"+ schemas+"\n5" +credentialDefs+"\n6"+revocStates+"\n fin\n");


            proofJson = Anoncreds.proverCreateProof(this.mainWallet, proofRequestJson, requestedCredentialsJson,
                    this.masterSecret, schemas, credentialDefs, revocStates).get();

            ProofCreationExtra proofCreationExtra = new
                    ProofCreationExtra(proofRequestJson, new JSONObject(proofJson).toString(4), schemas, credentialDefs);


            return proofCreationExtra;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    public DIDStructure getMainDID() {
        return mainDID;
    }


    public String returnVerifierGenerateSimpleProofRequestUnrestricted(String proofReqName, String proofReqVer,
                                                                       String[] requestedAttributes) {
        //generate proof request where  attributes issued by an endorser or self attested are requested
        //whitout specific schema/credential def requirements
        if (this.nonce == null) {
            try {
                this.nonce = Anoncreds.generateNonce().get();
                return IndyJsonStringBuilder.createSimpleProofRequestUnrestricted(this.nonce, proofReqName, proofReqVer, requestedAttributes);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (IndyException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String returnVerifierGenerateSimpleProofRequest(String proofReqName, String proofReqVer,
                                                           String[] requestedAttributes, String[] requiredSchema, String[]
                                                                   requiredCredentialDefinition
    ) {
        //optional:requiredSchema limits an attribute to belong to a specific schema issued by an IndyLibraries.Endorser
        //requiredCredentialDefinition limits the attribute to belong only to credential of that cred def
        //limiting it to be released only by a specific IndyLibraries.Endorser,
        // the requirments array form is ["id1,","","i2",...] where "" means no requirments
        //requestedAttributes.lenght must equal both requirments array
        if (requestedAttributes.length != requiredSchema.length &&
                requestedAttributes.length != requiredCredentialDefinition.length)
            throw new IllegalArgumentException("Wrong Lenght to requirments array");
        if (this.nonce == null) {
            try {
                this.nonce = Anoncreds.generateNonce().get();
                return IndyJsonStringBuilder.createSimpleProofRequest(this.nonce, proofReqName, proofReqVer, requestedAttributes,
                        requiredSchema, requiredCredentialDefinition);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (IndyException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //verify a proof created with non revocable credentials
    public Boolean returnVerifierVerifyProofNOREVOCATION(String proofRequestJson, String proofJson, String schemas,
                                                         String credentialDefs) {

        Boolean valid = false;
        String revocRegDefs = new JSONObject().toString(4);
        String revocRegs = new JSONObject().toString(4);
        try {
            valid = Anoncreds.verifierVerifyProof(proofRequestJson, proofJson, schemas, credentialDefs, revocRegDefs, revocRegs).get();
            //System.out.println("valid:"+valid);
            return valid;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        return valid;
    }

    public Boolean returnVerifierVerifyProof(String proofRequestJson, String proofJson, String schemas,
                                             String credentialDefs, String revocRegDefs,
                                             String revocRegs) {

        Boolean valid = false;
        try {
            if(revocRegs == null){
                revocRegs=new JSONObject("{}").toString();
            }
            if(revocRegDefs == null){
                revocRegDefs=new JSONObject("{}").toString();
            }

            valid = Anoncreds.verifierVerifyProof(proofRequestJson, proofJson, schemas, credentialDefs,
                    revocRegDefs,
                    revocRegs).get();
            return valid;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return valid;
    }

    public JSONObject generatePredicatesInfoForProofRequest(String name,
                                                            String predicateType,
                                                            String predicateValue,
                                                            String schema_id, String schema_issuer_did,
                                                            String schema_name,
                                                            String schema_version,
                                                            String issuer_credential_did,
                                                            String cred_def_id,
                                                            String rev_reg_id,
                                                            Long NonRevokedFROM, Long NonRevokedUNTIL) {
        return IndyJsonStringBuilder.generatePredicatesInfoForProofRequest(name, predicateType, predicateValue,
                schema_id, schema_issuer_did, schema_name, schema_version, issuer_credential_did, cred_def_id,
                rev_reg_id, NonRevokedFROM, NonRevokedUNTIL);
    }

    public static JSONObject generateAttrInfoForProofRequest(String name, String[] names,
                                                             String schema_id, String schema_issuer_did,
                                                             String schema_name,
                                                             String schema_version,
                                                             String issuer_credential_did,
                                                             String cred_def_id,
                                                             String[] attr_for_value_restrictions, String[] value_restriction_for_attrs, String rev_reg_id,
                                                             Long NonRevokedFROM, Long NonRevokedUNTIL) {
        return IndyJsonStringBuilder.generateAttrInfoForProofRequest(name, names, schema_id, schema_issuer_did,
                schema_name, schema_version,
                issuer_credential_did, cred_def_id, attr_for_value_restrictions,value_restriction_for_attrs,rev_reg_id, NonRevokedFROM, NonRevokedUNTIL);
    }

    public String returnVerifierGenerateProofRequest(String proofReqName, String proofReqVersion, String ver,
                                                     JSONObject[] requestedAttributes, JSONObject[] requestedPredicates,
                                                     Long NonRevokedFROM, Long NonRevokedUNTIL) {
        if (this.nonce == null) {
            try {
                this.nonce = Anoncreds.generateNonce().get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (IndyException e) {
                e.printStackTrace();
            }
        }

        return IndyJsonStringBuilder.createProofRequest(this.nonce, proofReqName, proofReqVersion, ver,
                requestedAttributes, requestedPredicates, NonRevokedFROM, NonRevokedUNTIL);

    }

    public ProofAttributesFetched returnProverSearchAttrForProof(String proofRequestJson,ArrayList<String> requestedRevealed) {
        //NECESSARIO CREARE UN'ALGORITMO CHE PER OGNI CREDENZIALE DEL CARO AGENT, CONTROLLI CHE CORRISPONDA AD
        //UNA REFERENCE CHE HA LUI
        JSONArray jsonArray;
        String credentialIdForAttribute, tmpcredId, tmpSchemaId, tmpRevRegistryId, tmpRevCredRegistryId;
        ArrayList<String> credentialID = new ArrayList<>();
        ArrayList<String> credentialIDForPredicates = new ArrayList<>();

        ArrayList<String> selfAttestedList = new ArrayList<>();
        ArrayList<String> AttrtoReferenceList = new ArrayList<>();

        ArrayList<String> credDefIDsAttr = new ArrayList<>();
        ArrayList<String> SchemaIDs = new ArrayList<>();
        ArrayList<String> RevocationRegistryForAttrIDs = new ArrayList<>();
        ArrayList<String> RevocationRegistryForPredIDs = new ArrayList<>();

        ArrayList<String> RevocationCredentialRegistryForAttrIDs = new ArrayList<>();
        ArrayList<String> RevocationCredentialRegistryForPredIDs = new ArrayList<>();

        ArrayList<String> predicatesCredDefIdList = new ArrayList<>();
        ArrayList<String> PredicatestoReferenceList = new ArrayList<>();
        HashMap<String,Boolean> toReveal = new HashMap<String, Boolean>();
        CredentialsSearchForProofReq credentialsSearch = null;
        try {
            System.out.println("PROOF REQUEST FOR RESEARCH" + proofRequestJson + "\n");
            JSONObject allEXtra=new JSONObject();
            JSONObject requested_attr= new JSONObject(proofRequestJson).getJSONObject("requested_attributes");
            for (String attrRef:
            requested_attr.keySet()) {
                allEXtra.put(attrRef,requested_attr.getJSONObject(attrRef));
            }
            credentialsSearch = CredentialsSearchForProofReq.open(this.mainWallet,
                    proofRequestJson, null).get();
            JSONObject jsonObject = new JSONObject(proofRequestJson).getJSONObject("requested_attributes");
            System.out.println("requested attr"+jsonObject.toString(4));
            for (String attrRef : jsonObject.keySet()) {
                if(requestedRevealed!=null)
                    toReveal.put(attrRef,requestedRevealed.contains(attrRef));
                jsonArray = new JSONArray(credentialsSearch.fetchNextCredentials(attrRef, 100).get());
                String credId;
                if (jsonArray.length() > 0) {
                    System.out.println("Wallet Stored Credential: " + jsonArray.getJSONObject(0).toString(4));
                    tmpSchemaId = jsonArray.getJSONObject(0).getJSONObject("cred_info").getString("schema_id");
                    tmpcredId = jsonArray.getJSONObject(0).getJSONObject("cred_info").getString("cred_def_id");
                    credentialIdForAttribute = jsonArray.getJSONObject(0).getJSONObject("cred_info").getString("referent");
                    tmpRevRegistryId = jsonArray.getJSONObject(0).getJSONObject("cred_info").optString("rev_reg_id");
                    if(!tmpRevRegistryId.equals("")) {
                        RevocationRegistryForAttrIDs.add(tmpRevRegistryId);
                        tmpRevCredRegistryId = jsonArray.getJSONObject(0).getJSONObject("cred_info").getString("cred_rev_id");
                        RevocationCredentialRegistryForAttrIDs.add(tmpRevCredRegistryId);
                    }
                    SchemaIDs.add(tmpSchemaId);

                    credDefIDsAttr.add(tmpcredId);
                    credentialID.add(credentialIdForAttribute);
                    AttrtoReferenceList.add(attrRef);

                } else {
                    System.out.println("attr can only be self attested");
                    selfAttestedList.add(attrRef);
                }
            }
            jsonObject = new JSONObject(proofRequestJson).getJSONObject("requested_predicates");

            for (String attrRef : jsonObject.keySet()) {
                jsonArray = new JSONArray(credentialsSearch.fetchNextCredentials(attrRef, 100).get());
                String credId;
                if (jsonArray.length() > 0) {
                    System.out.println("Wallet Stored Credential: " + jsonArray.getJSONObject(0).toString(4));
                    tmpSchemaId = jsonArray.getJSONObject(0).getJSONObject("cred_info").getString("schema_id");
                    tmpcredId = jsonArray.getJSONObject(0).getJSONObject("cred_info").getString("cred_def_id");
                    tmpRevRegistryId = jsonArray.getJSONObject(0).getJSONObject("cred_info").optString("rev_reg_id");
                    credentialIdForAttribute = jsonArray.getJSONObject(0).getJSONObject("cred_info").getString("referent");
                    if(!tmpRevRegistryId.equals("")) {
                        tmpRevCredRegistryId = jsonArray.getJSONObject(0).getJSONObject("cred_info").getString("cred_rev_id");
                        RevocationRegistryForPredIDs.add(tmpRevRegistryId);
                        RevocationCredentialRegistryForPredIDs.add(tmpRevCredRegistryId);
                    }
                    SchemaIDs.add(tmpSchemaId);
                    predicatesCredDefIdList.add(tmpcredId);
                    System.out.println("aa\naa" + jsonArray.getJSONObject(0).toString(4) + "aa");
                    credentialIDForPredicates.add(credentialIdForAttribute);
                    PredicatestoReferenceList.add(attrRef);

                }  {
                    System.out.println("No credential in Wallet for attribute of requested predicate reference : " +attrRef);
                }
            }
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            credentialsSearch.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ProofAttributesFetched(credentialID, selfAttestedList, AttrtoReferenceList,
                credDefIDsAttr, predicatesCredDefIdList, SchemaIDs, credentialIDForPredicates, PredicatestoReferenceList, RevocationRegistryForAttrIDs,
                RevocationRegistryForPredIDs, RevocationCredentialRegistryForAttrIDs, RevocationCredentialRegistryForPredIDs,toReveal);
    }

    public ProofCreationExtra proverCreateProof(ProofAttributesFetched fetchedAttrProofs,
                                                String proofRequestJson, String[] selfAttestedListValues
            , Long timestamp,
                                                int blobreaderhandle) {
        String requestedCredentialsJson = null;
        if (fetchedAttrProofs.selfAttestedList
                != null) {
            requestedCredentialsJson = IndyJsonStringBuilder.getProofBody(fetchedAttrProofs.selfAttestedList.toArray(String[]::new),
                    selfAttestedListValues, (String[]) (fetchedAttrProofs.AttrtoReferenceList.toArray(String[]::new)), (String[]) (fetchedAttrProofs.credentialID.toArray(String[]::new)),
                    fetchedAttrProofs.toReveal, (String[]) (fetchedAttrProofs.predicatestoReferenceList.toArray(String[]::new)), (String[]) (fetchedAttrProofs.predicateCredIDs.toArray(String[]::new)), timestamp);
        } else {
            requestedCredentialsJson = IndyJsonStringBuilder.getProofBody(null,
                    null, (String[]) (fetchedAttrProofs.AttrtoReferenceList.toArray(String[]::new)), (String[]) (fetchedAttrProofs.credentialID.toArray(String[]::new)),
                    fetchedAttrProofs.toReveal, (String[]) (fetchedAttrProofs.predicatestoReferenceList.toArray(String[]::new)), (String[]) (fetchedAttrProofs.predicateCredIDs.toArray(String[]::new)), timestamp);
        }
        //prendere dal json l'id dello schema e della creddef
        String schemas;
        JSONObject jsonObject = new JSONObject();
        for (String schemaId : (String[]) (new HashSet(fetchedAttrProofs.SchemaIDs).toArray(String[]::new))) {
            //necessario trovare la credential definition corrispondente allo schema
            //the credential definition must match the credential schema
            //System.out.println("schemaIDReqested "+schemaId +"\n ");
            JSONObject nullJobk = null;
            //jsonObject.put(schemaId, new JSONObject(getSchemaFromLedger(schemaId)).put("seqNo",nullJobk)).toString(4);
            jsonObject.put(schemaId, new JSONObject(getSchemaFromLedger(schemaId))).toString(4);

        }
        schemas = jsonObject.toString(4);
        jsonObject = new JSONObject();
        String credentialDefs;
        LedgerResults.ParseResponseResult responseResult = null;
        for (String credDefId : (String[]) (new HashSet(fetchedAttrProofs.AttrcredDefIDs)).toArray(String[]::new)) {
            //necessario trovare la credential definition corrispondente allo schema
            //the credential definition must match the credential schema
            //System.out.println("credDefIdRqeuqested "+credDefId+"\n  w");
            try {
                String req = Ledger.buildGetCredDefRequest(this.mainDID.didName, credDefId).get();
                String ris = Ledger.submitRequest(this.poolConnection, req).get();
                responseResult =
                        Ledger.parseGetCredDefResponse(ris).get();
            } catch (IndyException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            jsonObject.put(credDefId, new JSONObject(responseResult.getObjectJson())).toString(4);
        }
        for (String credDefId : (String[]) (new HashSet(fetchedAttrProofs.PredicatescredDefIDs)).toArray(String[]::new)) {
            //necessario trovare la credential definition corrispondente allo schema
            //the credential definition must match the credential schema
            //System.out.println("credDefIdRqeuqested "+credDefId+"\n  w");
            try {
                String req = Ledger.buildGetCredDefRequest(this.mainDID.didName, credDefId).get();
                String ris = Ledger.submitRequest(this.poolConnection, req).get();
                responseResult =
                        Ledger.parseGetCredDefResponse(ris).get();
            } catch (IndyException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            jsonObject.put(credDefId, new JSONObject(responseResult.getObjectJson())).toString(4);
        }
        credentialDefs = jsonObject.toString(4);
        String revocStates = new JSONObject().toString(4);
        String proofJson = "";
        String response = null;
        String request = null;
        JSONObject revStateObj = new JSONObject();
        String cred_rev_id;
        int i = 0;
        String revState = null;
        for (String revDefID : (String[]) (new HashSet(fetchedAttrProofs.AttrRevRegIDs)).toArray(String[]::new)) {
            //necessario trovare la credential definition corrispondente allo schema
            //the credential definition must match the credential schema
            //System.out.println("credDefIdRqeuqested "+credDefId+"\n  w");
            try {
                request = Ledger.buildGetRevocRegDeltaRequest(this.mainDID.didName,
                        revDefID, timestamp, timestamp).get(); // read the delta for the interval, which was requested in the proof request
                response = Ledger.submitRequest(this.poolConnection, request).get();
                LedgerResults.ParseRegistryResponseResult deltaResult = Ledger.parseGetRevocRegDeltaResponse(response).
                        get();
                String delta = deltaResult.getObjectJson();
                cred_rev_id = fetchedAttrProofs.AttrCREDENTIALRevRegIDs.get(i);
                i++;
                System.out.println("Read the delta from the ledger response:\n" + response + "\n");
                revState = Anoncreds.createRevocationState(blobreaderhandle,
                        getRevocationDefinition(revDefID), delta,
                        timestamp, cred_rev_id).get();//cred_rev_id identifies the credential in the corresponding
                //revocation registry


            } catch (IndyException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            revStateObj.put(revDefID, new JSONObject().put(Long.toString(timestamp)
                    , new JSONObject(revState)));
        }
        i = 0;

        for (String revDefID : (String[]) (new HashSet(fetchedAttrProofs.PredicateRevRegIDs)).toArray(String[]::new)) {
            //necessario trovare la credential definition corrispondente allo schema
            //the credential definition must match the credential schema
            //System.out.println("credDefIdRqeuqested "+credDefId+"\n  w");
            try {
                request = Ledger.buildGetRevocRegDeltaRequest(this.mainDID.didName,
                        revDefID, timestamp, timestamp).get(); // read the delta for the interval, which was requested in the proof request
                response = Ledger.submitRequest(this.poolConnection, request).get();
                LedgerResults.ParseRegistryResponseResult deltaResult = Ledger.parseGetRevocRegDeltaResponse(response).
                        get();
                String delta = deltaResult.getObjectJson();
                cred_rev_id = fetchedAttrProofs.PredicateCREDENTIALRevRegIDs.get(i);
                i++;
                System.out.println(cred_rev_id + "rev id");
                System.out.println("Read the delta from the ledger response:\n" + response + "\n");
                revState = Anoncreds.createRevocationState(blobreaderhandle,
                        getRevocationDefinition(revDefID), delta,
                        timestamp, cred_rev_id).get(); //cred_rev_id identifies the credential in the corresponding
                //revocation registry


            } catch (IndyException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            revStateObj.put(revDefID, new JSONObject().put(Long.toString(timestamp)
                    , new JSONObject(revState)));
        }

        if (this.masterSecret == null)
            this.createMasterSecret();
        System.out.println("masterSecret" + this.masterSecret);
        //System.out.println("inizio\n1"+proofRequestJson+"\n2"+requestedCredentialsJson+"\n3"+ this.masterSecret+"\n4"+ schemas+"\n5" +credentialDefs+"\n6"+revocStates+"\n fin\n");

        try {
            System.out.println("1:" + proofRequestJson + "\n2:" + requestedCredentialsJson + "\n3:"
                    + this.masterSecret + "\n4" + schemas + "\n5" + credentialDefs + "\n6" + revStateObj.toString(4));
            if(revStateObj.toString().equals("{}"))
                proofJson = Anoncreds.proverCreateProof(this.mainWallet, proofRequestJson, requestedCredentialsJson,
                        this.masterSecret,
                        schemas, credentialDefs,
                        new JSONObject("{}").
                                toString()).
                        get();

            else {
                proofJson = Anoncreds.proverCreateProof(this.mainWallet, proofRequestJson, requestedCredentialsJson,
                        this.masterSecret, schemas, credentialDefs, revStateObj.toString(4)).get();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }

        ProofCreationExtra proofCreationExtra = new
                ProofCreationExtra(proofRequestJson, new JSONObject(proofJson).toString(4), schemas, credentialDefs);

        return proofCreationExtra;
    }

    //PROVER NEEDS TO CREATE IT'S OWN REVOCATION DELTAS
    public String proverGetRevocationStateFORPROOF(RevocationRegistryObject revocationRegistryObject, long timestamp,
                                                   String revocationOFcredentialID) {
        String request, response;
        LedgerResults.ParseRegistryResponseResult deltaResult = null;
        try {

            // create the revocation states which participate in the proof
            request = Ledger.buildGetRevocRegDeltaRequest(null,
                    revocationRegistryObject.revRegId, timestamp,
                    timestamp).get(); // read the delta for the interval, which was requested in the proof request
            response = Ledger.submitRequest(this.poolConnection, request).get();
            System.out.println("Read the delta from the ledger response:\n" + response + "\n");

            deltaResult = Ledger.parseGetRevocRegDeltaResponse(response).get();
            String delta = deltaResult.getObjectJson();
            // the prover creates the revocaton state in the proverTimestamp moment
            String revState = Anoncreds.createRevocationState(revocationRegistryObject.blobStorageReaderHandle
                    , revocationRegistryObject.revRegDefJson, delta, timestamp, revocationOFcredentialID).get();
            JSONObject revStates = new JSONObject();
            revStates.put(revocationRegistryObject.revRegId, new JSONObject().put(Long.toString(timestamp), new JSONObject(revState)));
            System.out.println("The revocation states have been created\n" + revStates);
            return revStates.toString(4);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getRevocationDefinition(RevocationRegistryObject revreg) {
        String request, response;
        JSONObject revocRegDefs = new JSONObject();
        try {
            request = Ledger.buildGetRevocRegDefRequest(this.mainDID.didName, revreg.revRegId).get();
            response = Ledger.submitRequest(this.poolConnection, request).get();
            LedgerResults.ParseResponseResult parseResult = Ledger.parseGetRevocRegDefResponse(response).get();
            String revRegDefReadFromLedgerByVerifier = parseResult.getObjectJson();
            revocRegDefs = (new JSONObject(revRegDefReadFromLedgerByVerifier));
            System.out.println("Prover has build his own Revocation Defs:\n" + revocRegDefs + "\n");
            return revocRegDefs.toString(4);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getRevocationDefinition(String revRegId) {
        String request, response;
        JSONObject revocRegDefs = new JSONObject();
        try {
            request = Ledger.buildGetRevocRegDefRequest(this.mainDID.didName, revRegId).get();
            response = Ledger.submitRequest(this.poolConnection, request).get();
            LedgerResults.ParseResponseResult parseResult = Ledger.parseGetRevocRegDefResponse(response).get();
            return parseResult.getObjectJson();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        return null;
    }


    public String getDeltaForProof(RevocationRegistryObject revRegSub, long time) {
        String request, response;
        try {
            request = Ledger.buildGetRevocRegDeltaRequest(this.mainDID.didName, revRegSub.revRegId, time, time).get();
            response = Ledger.submitRequest(this.poolConnection, request).get();
            System.out.println("Prover has read the revoc delta for interval from: " + time + "to: " + time + " response from ledger \n" + response + "\n");
            LedgerResults.ParseRegistryResponseResult parseRegRespResult = parseGetRevocRegDeltaResponse(response).get();
            String proverReadDeltaFromLedger = parseRegRespResult.getObjectJson();
            System.out.println("Prover has read the revoc delta for interval from: " + time + "to: " + time + "\n" + proverReadDeltaFromLedger + "\n");
            return proverReadDeltaFromLedger;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
        return null;
    }

    //qualify DID assign a DID to a specific DID method, base idea is to store it as indy
    //NOTE:QUALIFIED DID ARE ONLY IN THE LEDGER, THEY ARE TRANSLATED TO UNQUALIFIED IN THE LEDGER
    //IT'S NOT ALLOWED TO HAVE  PREFIX IN THE LEDGER FOR MORE FLEXIBILITY
    public String qualifyDID() {
        try {
            String qualifiedDID = Did.qualifyDid(this.mainWallet, this.mainDID.didName, "indy").get();
            this.mainDID.didName = qualifiedDID;
            return qualifiedDID;
        } catch (WalletItemAlreadyExistsException e) {
            return this.mainDID.didName;
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    //qualify my DID
    public String qualifyDID(String method) {
        try {
            String qualifiedDID = Did.qualifyDid(this.mainWallet, this.mainDID.didName, method).get();

        } catch (WalletItemAlreadyExistsException e) {
            return this.mainDID.didName;
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;

    }

    //requires:their DID already Stored in Wallet
    public String qualifyDID(String therDID, String method) {
        try {
            return Did.qualifyDid(this.mainWallet, this.mainDID.didName, method).get();
        } catch (WalletItemAlreadyExistsException e) {
            return this.mainDID.didName;
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;

    }

    //setting an endpoint for connecting to the agent behind the DID
    public boolean setDIDEndpoint(String address) {
        try {
            Did.setEndpointForDid(mainWallet, mainDID.didName, address, mainDID.didVerKey).get();
            return true;
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    //getting an endpoint for connecting to the agent behind the DID
    public String getDIDEndpoint(String theirDID) {
        //theirDID needs to be already stored into the wallet and published on ledger
        try {
            DidResults.EndpointForDidResult endpointres = Did.
                    getEndpointForDid(this.mainWallet, this.poolConnection, theirDID).
                    get();
            return endpointres.getAddress() + " " + endpointres.getTransportKey();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
    //check endpoint for my DID
    public String getDIDEndpoint() {
        try {
            DidResults.EndpointForDidResult endpointres = Did.getEndpointForDid(this.mainWallet, this.poolConnection, this.mainDID.didName).get();
            return endpointres.getAddress() + " " + endpointres.getTransportKey();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean setDIDMetadata(String metaDATA) {
        try {
            Did.setDidMetadata(this.mainWallet,this.mainDID.didName,"").get();
            return true;
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }
    public String getDIDMetadata(){
        try{
            return Did.getDidMetadata(this.mainWallet,this.mainDID.didName).get();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getTheirDIDMetadata(String theirDID){
        try{
            return Did.getDidMetadata(this.mainWallet,theirDID).get();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getDIDWithMetadata(){
        try{
            return Did.getDidWithMeta(this.mainWallet,this.mainDID.didName).get();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
    public String getTheirDIDWithMetadata(String theirDID){
        try{
            return Did.getDidWithMeta(this.mainWallet,theirDID).get();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }


    public String createDIDwithEndpoint(String EndPointAddress) {
        //create DID based on Seed
            String DID = null;
            String DIDVerkey = null;
            DIDStructure didStructure;
            DidResults.CreateAndStoreMyDidResult AgentDIDResult =
                    null;
            try {
                AgentDIDResult = Did.createAndStoreMyDid(mainWallet,
                        IndyJsonStringBuilder.createDIDString(null, null
                        , null, null, "indy")).get();
                Did.setEndpointForDid(mainWallet,AgentDIDResult.getDid(),EndPointAddress,AgentDIDResult.getVerkey()).get();
                //this.setDIDMetadata("MYMETA-ADMIN-DATA");

                this.agentsFile.insertAgentDID(this.agentName, AgentDIDResult.getDid());
                this.agentsFile.insertAgentVerKey(this.agentName, AgentDIDResult.getVerkey());
                this.agentsFile.makeBackup();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (IndyException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        return addDIDToCollectionSetMainDid(AgentDIDResult,EndPointAddress);
        }

    private String addDIDToCollectionSetMainDid(DidResults.CreateAndStoreMyDidResult agentDIDResult, String endPointAddress) {
        String DID;
        String DIDVerkey;
        DIDStructure didStructure;
        if (agentDIDResult == null)
            return null;
        DID = agentDIDResult.getDid();
        DIDVerkey = agentDIDResult.getVerkey();
        didStructure = new DIDStructure(DID, DIDVerkey,endPointAddress);
        if (mainDID == null) {
            mainDID = didStructure;
        }
        DIDCollection.put("DID", didStructure);
        return DID;
    }

    private String addDIDToCollectionSetMainDid(String did, String verkey) {
        String DID;
        String DIDVerkey;
        DIDStructure didStructure;
        if (verkey == null || did == null)
            return null;
        DID = did;
        DIDVerkey = verkey;
        didStructure = new DIDStructure(DID, DIDVerkey);
        if (mainDID == null) {
            mainDID = didStructure;
        }
        DIDCollection.put("DID", didStructure);
        return DID;
    }

    public String getEndPointFromLedger(String did){
        String endpointFromLedger=this.getATTRIBFromLedger(did,"endpoint");
        JSONObject jsonObject = new JSONObject(endpointFromLedger);
        System.out.println("endpoint\n"+
                endpointFromLedger);

        String endpointdata =jsonObject.getJSONObject("result").getString("data");
        String jsonObjectENDPOINT = new JSONObject(endpointdata).getJSONObject("endpoint").getString("did2did");
        //System.out.println("endpoint=IP:PORT"+jsonObjectENDPOINT);
        return jsonObjectENDPOINT;
    }
    public String getDIDVerKeyFromLedger (String theirDID){
        String dataResult = new JSONObject(this.getNYMFromLedger(theirDID)).getString("data");
        String verKey= new JSONObject(dataResult).getString("verkey");

                //.get("verkey").toString();
        System.out.println(verKey);
        return verKey;
    }

    public String createConnection(Socket connectionSocket,String agentName)  {
        DidResults.CreateAndStoreMyDidResult AgentDIDResult
                = null;
        try {
            AgentDIDResult = Did.createAndStoreMyDid(mainWallet, IndyJsonStringBuilder.getEmptyJson()).get();
            return DID2DIDComm.askForDID2DIDCommunication(this.mainWallet,connectionSocket,AgentDIDResult.getDid(),AgentDIDResult.getVerkey(),
                    agentName);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public String createDID2DIDCommunication(String agentName, InetSocketAddress agentAddress, DatagramSocket ds)  {
        DidResults.CreateAndStoreMyDidResult AgentDIDResult
                = null;
        String theirDID=null;
        byte[] toSend= null;
        try {
            AgentDIDResult = Did.createAndStoreMyDid(mainWallet, IndyJsonStringBuilder.getEmptyJson()).get();
            toSend=DID2DIDComm.setupDID2DIDCommunicationAsk(AgentDIDResult.getDid(),AgentDIDResult.getVerkey(),
                    agentName);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try  {
            DatagramPacket datagramPacket = new DatagramPacket(toSend, toSend.length,
                    agentAddress.getAddress(),agentAddress.getPort());
            ds.send(datagramPacket);
            byte[]datagramBuffer=new byte[65535];
            DatagramPacket toReceive=new DatagramPacket(datagramBuffer,datagramBuffer.length);
            ds.receive(toReceive);
            int i;
            byte [] dataArray = new byte[toReceive.getLength()];
            for(i= 0 ;i <toReceive.getLength();i++ ){
                dataArray[i]=datagramBuffer[i];
            }
            theirDID=DID2DIDComm.setupDID2DIDCommunicationResponse(mainWallet,AgentDIDResult.getDid(),dataArray);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return theirDID;
    }
    //returns the DID created on this agent side for DID2DID comunication and the message in byte to send to the remote Agent
    public AskDID2DIDresult createDID2DIDCommunicationAsk(String agentName) {
        DidResults.CreateAndStoreMyDidResult AgentDIDResult
                = null;
        String theirDID = null;
        byte[] toSend = null;
        try {
            AgentDIDResult = Did.createAndStoreMyDid(mainWallet, IndyJsonStringBuilder.getEmptyJson()).get();
            toSend = DID2DIDComm.setupDID2DIDCommunicationAsk(AgentDIDResult.getDid(), AgentDIDResult.getVerkey(),
                    agentName);
            return new AskDID2DIDresult(AgentDIDResult, toSend);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
        //process answer to DID2DID commnunication response given and returns theirDID,
        //given AskDID2DIDresult takes the DID to the current Agent and uses it to create a Pairwise with theirDID from the remote
        // agent
        //
    public String createDID2DIDCommunicationResponse(AskDID2DIDresult askdid2did,byte[]dataReceived) {
        try {
            String theirDID=DID2DIDComm.setupDID2DIDCommunicationResponse(mainWallet,askdid2did.agentDIDResult.getDid(),dataReceived);
        } catch (IndyException ex) {
            ex.printStackTrace();
        } catch (ExecutionException ex) {
            ex.printStackTrace();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        return null;
    }


    public String readMessage(byte[] message){
        //read authenticated message
        try {
            return DID2DIDComm.readDID2DIDMSG(this.mainWallet,message);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
    public byte[] writeMessage(String message,String theirDID){
        //write authenticated message
        try {
            return DID2DIDComm.writeDID2DIDMSG(this.mainWallet,theirDID,message);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }


    public String acceptConnection(String receivedMsg, String agentName) {
        DidResults.CreateAndStoreMyDidResult AgentDIDResult
                = null;
        try {
            AgentDIDResult = Did.createAndStoreMyDid(mainWallet, IndyJsonStringBuilder.getEmptyJson()).get();
            //return own DID TO DID information
            return DID2DIDComm.createDIDtoDID(this.mainWallet,receivedMsg,AgentDIDResult.getDid(),
                    AgentDIDResult.getVerkey(),agentName);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public void sendD2DMessage(byte[] msg,Socket connectionSocket) {
        try {
            DID2DIDComm.sendMSG(msg, connectionSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public byte[] waitForMessage(Socket connectionSocket){
        try {
            return DID2DIDComm.receiveMSG(connectionSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public byte[] askForCredentialMSG(String theirDID){
        //ask for a credential to an Endorser, Endorser will then send a Credential Offer !

        JSONObject request = new JSONObject();
        request.put("request","Cred_Request_0");

       return this.writeMessage(request.toString(4),theirDID);
    }
    public byte[] credentialRequestMSG(String theirDID,String credReq,String credOffer){
        //ask for a credential to an Endorser, Endorser will then send a Credential Offer !

        JSONObject request = new JSONObject();
        request.put("request","Cred_Request_1");
        request.put("credReq",credReq);
        request.put("credOffer",credOffer);
        return this.writeMessage(request.toString(4),theirDID);
    }
    public String generateNonce(){
        try {
            return Anoncreds.generateNonce().get();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
    //@param credentialId is the Identifier by which requested credential is stored in the wallet
    public boolean proverDeleteCredential(String credentialId){
        try {
            Anoncreds.proverDeleteCredential(this.mainWallet,credentialId).get();
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

    //when issuing a revocation state, it's needed that the revDelta covers all the whole registy existance time
    //meaning from '0' to:'needed time', after updateRevocationState it is then possibile to update this
    //revocation state further, with from'timestampOfRevocation' and to:'needed time',
    //the get the specific delta call getRevocationRegistryDeltaSpecificTimeFrame.
    //NOTE:revDelta can be obtained by call issuerCreateCredential or issuerRevokeCredential.
    //NOTE2:revDelta must be updated to needed time to make sure that a revoked credential is not accounted for
    //when a creden tial is revoked for the first time in a revocation registry it will be enough to use the
    //revocation registry from the first issuerCreateCredential associated with the registry
    public String createRevocationState(RevocationRegistryObject revregObject,
                                        String revDelta,long timestampOfRevocation,String
                                                cred_rev_id){
        try {
            return Anoncreds.createRevocationState(revregObject.blobStorageReaderHandle,revregObject.revRegDefJson ,revDelta
                    ,timestampOfRevocation,
                    cred_rev_id).get();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;

    }
}
