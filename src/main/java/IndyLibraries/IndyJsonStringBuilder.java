package IndyLibraries;

import org.json.*;

public class IndyJsonStringBuilder {
    private final static String emptyJson="{}";

    public IndyJsonStringBuilder(){}
    /*
    	 * @param didJson Identity information as json.
	 * {
	 *     "did": string, (optional;
	 *             if not provided and cid param is false then the first 16 bit of the verkey will be used as a new DID;
	 *             if not provided and cid is true then the full verkey will be used as a new DID;
	 *             if provided, then keys will be replaced - key rotation use case)
	 *     "seed": string, (optional) Seed that allows deterministic did creation (if not set random one will be created).
	 *                                Can be UTF-8, base64 or hex string.
	 *     "crypto_type": string, (optional; if not set then ed25519 curve is used;
	 *               currently only 'ed25519' value is supported for this field)
	 *     "cid": bool, (optional; if not set then false is used;)
	 *     "method_name": string, (optional) method name to create fully qualified did.
	 * }
    */
    public static String createDIDString(String didValue,String seedValue,String cryptotypeValue,
                                         Boolean cidValue,String methodnameValue){
        JSONObject jsonObject= new JSONObject();

        //every value is optional
        if(didValue!=null){
            jsonObject.accumulate("did",didValue);
        }
        if(seedValue!=null) {
            jsonObject.accumulate("seed", seedValue);
        }
        /*ignore cryptotypeValue, only 'ed25519' is supported
        if(cryptotypeValue!=null) {
            jsonObject.append("crypto_type", cryptotypeValue)
        }*/
        if(cidValue !=null){
            jsonObject.accumulate("cid",cidValue.booleanValue());
        }
        if(methodnameValue!=null){
            jsonObject.accumulate("method_name",methodnameValue);
        }
        if(jsonObject.length() == 0)
            return emptyJson;
        return jsonObject.toString(4);
    }
    public static String getEmptyJson(){
        return emptyJson;
    }
    /*
    	 * Builds a SCHEMA request. Request to add Credential's schema.
	 *                    {
	 *                         id: identifier of schema
                *                         attrNames: array of attribute name strings (the number of attributes should be less or equal than 125)
                *                         name: Schema's name string
                *                         version: Schema's version string,
                *                         ver: Version of the Schema json
                *   }
      */
    public static String createSchemaDataString(String submitterDIDValue,String idValue,String nameValue,
                                            String versionValue, String verValue,
                                              String[] attrNamesValues){
        JSONObject jsonObjectData = new JSONObject();
        jsonObjectData.put("id", idValue);
        jsonObjectData.put("version", versionValue);
        jsonObjectData.put("name",nameValue);
        jsonObjectData.put("ver", verValue);
        jsonObjectData.put("attrNames",attrNamesValues);
        return jsonObjectData.toString(4);
    }
    /*
    public static String createSchemaDataString(String submitterDIDValue,String idValue,String nameValue,
                                                String versionValue, String verValue,
                                                String[] attrNamesValues,String seqNoValue){
        //metodo nel caso della creazione di un json che rappresenta uno schema per la
        //creazione di una credential definition
        JSONObject jsonObjectData = new JSONObject();
        jsonObjectData.put("id", idValue);
        jsonObjectData.put("version", versionValue);
        jsonObjectData.put("name",nameValue);
        jsonObjectData.put("ver", verValue);
        jsonObjectData.put("attrNames",attrNamesValues);
        //campo extra opzionale che specifica il numero di sequenza della transazione dove è stato
        //pubblicato lo schema ( la SCHEMA transaction specifica)
        if(seqNoValue!=null) {
            jsonObjectData.put("seqNo", seqNoValue);
        }
        return jsonObjectData.toString(4);
    }*/
    public static String createSchemaAttributesString(
                                                String[] attrNamesValues) {
        JSONArray jsonArray= new JSONArray(attrNamesValues);
        return jsonArray.toString(4);
    }



    public static String typeSpecificConfigCredDef(boolean supportRevocationValue){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("support_revocation",supportRevocationValue);
        return jsonObject.toString(4);
    }

    public static String createCredDefJson(String storeCredDef){

        JSONObject jsonObject = new JSONObject(storeCredDef);
        return jsonObject.toString(4);
    }


    public static String getCredDefIdJson(String credDefIdValue) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("cred_def_id", credDefIdValue);
        return jsonObject.toString(4);
    }

    public static String getProofBody(String[]selfAttestedRef,String[] selfAttestedValue,String[]requestedAttributeRefent,
                                      String[] requestedAttributeCredId,
                                      String[] requestedAttributeRevevaledOrNot,
                                      String[]requestedPredicatesRefent,
                                      String[] requestedPredicatesCredId,Long time){
        JSONObject jsonObject,jsonObject2;
        jsonObject=new JSONObject();
        jsonObject2=new JSONObject();
        int i;
        if(selfAttestedRef!=null && selfAttestedValue!=null) {
            for (i = 0; i < selfAttestedRef.length; i++) {
                jsonObject2 = jsonObject2.put(selfAttestedRef[i], selfAttestedValue[i]);
            }
            jsonObject.put("self_attested_attributes",jsonObject2);
        }
        else
            jsonObject.put("self_attested_attributes",new JSONObject());

        jsonObject2=new JSONObject();
        if(requestedAttributeCredId!=null && requestedAttributeRefent!=null
        &&requestedAttributeRevevaledOrNot !=null) {
            //System.out.println(requestedAttributeRefent.length + " "+requestedAttributeCredId.length + " "+ requestedAttributeRevevaledOrNot.length);
            //System.out.println((requestedAttributeRefent.length-1)+" "+ requestedAttributeRefent[requestedAttributeRefent.length-1]);
            for (i = 0; i < requestedAttributeRefent.length; i++) {
                //System.out.println("index i: "+ i + " " +requestedAttributeRefent.length + " "+requestedAttributeCredId.length + " "+ requestedAttributeRevevaledOrNot.length);
                JSONObject toInsert= new JSONObject().put("cred_id", requestedAttributeCredId[i]).put("revealed",
                        (Boolean) requestedAttributeRevevaledOrNot[i].equals("true"));
                if(time!=null){
                    toInsert.put("timestamp",time);
                }
                jsonObject2.put(requestedAttributeRefent[i],
                        toInsert);
            }
            jsonObject.put("requested_attributes",jsonObject2);
        }
        else jsonObject.put("requested_attributes",new JSONObject());


        if(requestedPredicatesCredId!=null && requestedPredicatesRefent!=null){
            jsonObject2=new JSONObject();

            for (i = 0; i < requestedPredicatesRefent.length; i++) {

                JSONObject toInsert= new JSONObject().put("cred_id", requestedPredicatesCredId[i]);
                if(time!=null){
                    toInsert.put("timestamp",time);
                }
                jsonObject2.put(requestedPredicatesRefent[i],
                        toInsert);
            }
            jsonObject.put("requested_predicates",jsonObject2);
        }
        else
            jsonObject.put("requested_predicates",new JSONObject());
        return jsonObject.toString(4);
    }

    public static String createCredentialValues(String[] credKeys, String[] credValues) {
        JSONObject jsonObject = new JSONObject();
        int i;
        for ( i = 0; i < credKeys.length; i++) {
            jsonObject.put(credKeys[i],credValues[i]);
        }
        return jsonObject.toString(4);
    }
    //Metodo che crea una proof request dove non è specificato lo schema/issuer che pubblica la credenziale,
    //è accettato qualsasi schema che contiene un attributo
    public static String createSimpleProofRequestUnrestricted(String nonce, String proofReqName, String proofReqVersion, String[] requestedAttributes) {
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("nonce",nonce);
        jsonObject.put("name",proofReqName);
        jsonObject.put("version",proofReqVersion);
        JSONObject jsonObject2= new JSONObject();
        int i=0;
        for (String reqAttr: requestedAttributes
             ) {
            jsonObject2.put("attr"+String.valueOf((i++)+1)+"_referent",new JSONObject().put("name",reqAttr));
        }
        jsonObject.put("requested_attributes",jsonObject2);
        return jsonObject.toString(4);
    }
    //vengono specificati per ogni attributo richiesto issuer o schema id (quindi requestedAttribute[i] avrà
    //specificato schema_id[i] (se !="") e issuer_id[i](se !="")
    public static String createSimpleProofRequest(String nonce, String proofReqName, String proofReqVersion,
                                                  String[] requestedAttributes, String[] reqAttrSchema,
                                                  String[] reqAttrCredDef) {
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("nonce",nonce);
        jsonObject.put("name",proofReqName);
        jsonObject.put("version",proofReqVersion);
        JSONObject jsonObject2= new JSONObject();
        JSONObject jsonObject3= new JSONObject();
        int i=0;
        if(requestedAttributes.length != reqAttrSchema.length
        && reqAttrSchema.length == reqAttrCredDef.length)
            return null;
        for (String reqAttr: requestedAttributes
        ) {
            if(reqAttrSchema[i]!="" || reqAttrCredDef[i]!="") {
                if(reqAttrSchema[i]!="")
                    jsonObject3.put("schema_id",reqAttrSchema[i] );
                if(reqAttrCredDef[i]!=""){
                    jsonObject3.put("cred_def_id",reqAttrCredDef[i] );

                }
                jsonObject2.put("attr" + ((i++) + 1) + "_referent", new JSONObject().put("name", reqAttr).put("restrictions",jsonObject3));
            }
            else
                jsonObject2.put("attr" + ((i++) + 1) + "_referent", new JSONObject().put("name", reqAttr));

        }
        jsonObject.put("requested_attributes",jsonObject2);
        return jsonObject.toString(4);
    }

    public static String createRevocationRegistryConfig(String issuranceType, Integer maxCredRegistryProcess) {
        JSONObject jsonObject = new JSONObject();
        if(issuranceType!=null) {
                if(maxCredRegistryProcess!=null) {
                    jsonObject.put("issuance_type", issuranceType).
                            put("max_cred_num", maxCredRegistryProcess);
                }
                else{
                    jsonObject.put("issuance_type", issuranceType);
                }
            }
        else if(maxCredRegistryProcess!=null) {
            jsonObject.put("max_cred_num",maxCredRegistryProcess);

        }
        return jsonObject.toString();
    }
    /*A complete proof request uses a wql query in the proof request to
    limit to a specific version of a schema and/or cred_def but also for others restrictions
     wql query: indy-sdk/docs/design/011-wallet-query-language/README.md
	 *     The list of allowed fields:
	 *         "schema_id": <credential schema id>,
	 *         "schema_issuer_did": <credential schema issuer did>,
	 *         "schema_name": <credential schema name>,
	 *         "schema_version": <credential schema version>,
	 *         "issuer_did": <credential issuer did>,
	 *         "cred_def_id": <credential definition id>,
	 *         "rev_reg_id": <credential revocation registry id>, // "None" as string if not present
     */
    public static String createProofRequest(String nonce, String proofReqName, String proofReqVersion,String ver,
                                                  JSONObject[] requestedAttributes,JSONObject[] requestedPredicates,
                                            Long NonRevokedFROM,Long NonRevokedUNTIL
                                                  ) {
        JSONObject jsonObjectRequest=new JSONObject();

        JSONObject jsonObject=new JSONObject();
        JSONObject jsonObject2=null;
        int j;
        int i=0;
        jsonObjectRequest.put("nonce",nonce);
        jsonObjectRequest.put("name",proofReqName);
        jsonObjectRequest.put("version",proofReqVersion);
        if( requestedAttributes!=null ) {
            for (i = 0; i < requestedAttributes.length; i++) {
                jsonObject.put("attr" + (i) + "_referent", requestedAttributes[i]);
            }
        }
        jsonObjectRequest.put("requested_attributes",jsonObject);
        jsonObject=new JSONObject();
        j=i;
        if(requestedPredicates!=null){
            for ( i = 0; i < requestedPredicates.length ; i++) {
                jsonObject.put("pred"+(j++)+"_referent",requestedPredicates[i]);
            }
        }
        jsonObjectRequest.put("requested_predicates",jsonObject);
        jsonObject2=new JSONObject();
        if(NonRevokedFROM != null){
            jsonObject2.put("from",NonRevokedFROM);
        }
        if(NonRevokedUNTIL !=null){
            jsonObject2.put("to",NonRevokedUNTIL);
        }
        if(jsonObject2.length()>0){
            jsonObjectRequest.put("non_revoked",jsonObject2);
        }
        if(ver!=null){//ver can be either 1.0(default) or 2.0, 2.0 specify fully qualified attributes for restrictions
            //where fully qualified identifier means:<prefix>:<method>:<value>, prefix specify entity type,
            //method specify the network the entity belongs to
            // value: it is the main part of the qualifier (what would be seen if the identifier were unqualified)
            //https://github.com/hyperledger/indy-sdk/blob/master/docs/how-tos/fully-qualified-did-support.md
            jsonObjectRequest.put("ver",ver);
        }
        return jsonObjectRequest.toString(4);
    }

    public static JSONObject generateAttrInfoForProofRequest(String name,String[]names,
                                                  String schema_id,String schema_issuer_did,
                                                  String schema_name,
                                                  String schema_version,
                                                  String issuer_credential_did,
                                                  String cred_def_id,
                                                  String rev_reg_id,
                                                  Long NonRevokedFROM,Long NonRevokedUNTIL){
    //name or names at least
        // one of them should be used and at most one of them can be used,
        // names is used if we want to match attributes from ONE CREDENTIAL ( the credential needs to provide them all)
        //[NonRevokedFROM,NonRevokedUNTIL] specifies a timeframe where the prover must have the credential whitout it beeing
        //revoked, one or both could be null
        JSONObject jsonObject = new JSONObject();
        JSONObject jsonObject2 = new JSONObject();
        JSONObject jsonRestrictionsCollection = new JSONObject();
        if(name==null && names==null || (name != null && names != null)){
            return null;//update with throw new IllegalArgumentException
        }
        else if(name!=null){
            jsonObject.put("name",name);
        }
        else{
            jsonObject.put("names",names);
        }
        if(schema_id!=null){
            jsonRestrictionsCollection.put("schema_id",schema_id);
        }
        if(schema_issuer_did!=null){
            jsonRestrictionsCollection.put("schema_issuer_did",schema_issuer_did);
        }
        if(schema_name!=null){
            jsonRestrictionsCollection.put("schema_name",schema_name);
        }
        if(schema_version!=null){
            jsonRestrictionsCollection.put("schema_version",schema_version);
        }
        if(issuer_credential_did!=null){
            jsonRestrictionsCollection.put("issuer_did",issuer_credential_did);
        }
        if(cred_def_id!=null){
            jsonRestrictionsCollection.put("cred_def_id",cred_def_id);
        }
        if(rev_reg_id!=null){
            jsonRestrictionsCollection.put("rev_reg_id",rev_reg_id);
        }
        if(jsonRestrictionsCollection.length()>0){
            jsonObject.put("restrictions",jsonRestrictionsCollection);
        }
        if(NonRevokedFROM != null){
                jsonObject2.put("from",NonRevokedFROM);

        }
        if(NonRevokedUNTIL !=null){
            jsonObject2.put("to",NonRevokedUNTIL);
        }
        if(jsonObject2.length()>0){
            jsonObject.put("non_revoked",jsonObject2);
        }

        return jsonObject;
    }
    public static JSONObject generatePredicatesInfoForProofRequest(String name,
                                                      String predicateType,
                                                      String predicateValue,
                                                      String schema_id,String schema_issuer_did,
                                                      String schema_name,
                                                      String schema_version,
                                                      String issuer_credential_did,
                                                      String cred_def_id,
                                                      String rev_reg_id,
                                                      Long NonRevokedFROM,Long NonRevokedUNTIL) {
        //predicateType:(">=", ">", "<=", "<") -IndySDK plans seems to be to only support ordering predicates-
        //https://github.com/hyperledger/indy-sdk/issues/923 <-info from

        //predicateValue: x <-specifies the value to compare the attribute value with
        JSONObject jsonObject = new JSONObject();
        JSONObject jsonObject2 = new JSONObject();
        JSONObject jsonRestrictionsCollection = new JSONObject();
        if (name == null) {
            return null;//update with throw new IllegalArgumentException
        }
        jsonObject.put("name", name);
        jsonObject.put("p_type", predicateType);
        jsonObject.put("p_value", Integer.parseInt(predicateValue));
        if(schema_id!=null){
            jsonRestrictionsCollection.put("schema_id",schema_id);
        }
        if(schema_issuer_did!=null){
            jsonRestrictionsCollection.put("schema_issuer_did",schema_issuer_did);
        }
        if(schema_name!=null){
            jsonRestrictionsCollection.put("schema_name",schema_name);
        }
        if(schema_version!=null){
            jsonRestrictionsCollection.put("schema_version",schema_version);
        }
        if(issuer_credential_did!=null){
            jsonRestrictionsCollection.put("issuer_did",issuer_credential_did);
        }
        if(cred_def_id!=null){
            jsonRestrictionsCollection.put("cred_def_id",cred_def_id);
        }
        if(rev_reg_id!=null){
            jsonRestrictionsCollection.put("rev_reg_id",rev_reg_id);
        }
        if(jsonRestrictionsCollection.length()>0){
            jsonObject.put("restrictions",jsonRestrictionsCollection);
        }
        if (NonRevokedFROM != null) {
            jsonObject2.put("from", NonRevokedFROM);

        }
        if (NonRevokedUNTIL != null) {
            jsonObject2.put("to", NonRevokedUNTIL);
        }
        if (jsonObject2.length() > 0) {
            jsonObject.put("non_revoked", jsonObject2);
        }

        return jsonObject;
    }

    public static String buildRawAttrJSON(String[] rawID, String[] rawValues) {
        if (rawID.length!=rawValues.length){
            return null;
        }
        JSONObject jsonObject =new JSONObject();
        int i;
        for ( i = 0; i < rawID.length; i++) {
            jsonObject.put(rawID[i],rawValues[i]);
        }
        return jsonObject.toString();
    }

    public static String endpointJson(String endpointName, String endpointAddress) {
        return new JSONObject().put("endpoint",
                (new JSONObject().put(endpointName,endpointAddress))).toString(4);
    }

    public static String create_nym_endorserRequest(String didName, String verkey, String trustAnchor) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("request","NYMENDORSER");
        jsonObject.put("fields",new JSONObject().put("did",didName).put("verkey",verkey).put("trustAnchor",trustAnchor));
        return jsonObject.toString(4);
    }
}
