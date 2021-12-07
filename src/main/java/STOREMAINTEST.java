import IndyLibraries.*;

import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class STOREMAINTEST {
    public static void main(String[] args) {

        System.out.println("Insert a known Steward DID to ask for it's endpoint in " +
                "the ledger");
        Scanner sc= new Scanner(System.in);
        String StewardSeed=sc.next();

        //Store store = new Store("INDYSCANPOOL","nomeStore123",StewardSeed); //Primo avvio store con richiesta ruolo steward
        Store store = new Store("INDYSCANPOOL",
                "nomeStore123","DqXASouuvUvUs4tD5CtFvh",
                "nomeStore",
                "abcd");
        store.addItem("Penna");

        /*
        //store.StoreIndy.getStoredDIDandVerkey("DqXASouuvUvUs4tD5CtFvh");
        //attenzione! il nome degli schema deve essere solo in lettere
        SchemaStructure s1=store.StoreIndy.publishschema("spedizioneterza","1.0",new String[]{"attr","item"});
        CredDefStructure c1=store.StoreIndy.IssuerCreateStoreAndPublishCredDef("tag",false,
                s1.schemaId);
        CredOfferStructure off1= store.StoreIndy.returnCredentialOffer(c1.credDefId);
        // OPERAZIONE : CREDENTIAL SCHEMA creato dallo store per ogni suo pacco

        File agentsFile=new File("./"+"agentsFile"+".json");
        JSONUserCredentialStorage jsonStoredCred= null;
        try {
            jsonStoredCred = new JSONUserCredentialStorage(agentsFile);
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //store.inizializecred(s1,c1,off1,null);

        Agent receiver= new Agent(store.pool, "agent",jsonStoredCred);
        //receiver.CreateWallet("rec","aa");
        receiver.OpenWallet("rec","aa");
        receiver.createDID();
        //CredRequestStructure crs1=receiver.returnproverCreateCredentialRequest(store.offerToClient);
        /*CreateCredentialResult ccr1=store.StoreIndy.returnIssuerCreateCredentialNonRevocable
                (new String[]{"attr","item"},new String[]{"attr","item"},store.offerToClient.credOffer,crs1.credReqJson);*/
        //System.out.println("ccr1"+ccr1.credentialJson);
        //store.inizializecred(s1,c1,off1,crs1);

        /*

          SchemaStructure SpedizioneAnonimaSchema =store.createPackageCredentialSchemaAndPublish(
                "speedizione","1.0",new String[]{"attr"});



        CredDefStructure SpedizioneAnonimaCredDef = store.createPackageCredentialDefinitionAndPublish
                ("tag",false,SpedizioneAnonimaSchema.schemaId);

        SchemaStructure SpedizioneAnonimaSchema = store.packageSchema;
        CredDefStructure SpedizioneAnonimaCredDef = store.packageCredential;
        Agent receiver= new Agent(store.pool, "agent",jsonStoredCred);
        //receiver.CreateWallet("rec","aa");
        receiver.OpenWallet("rec","aa");
        receiver.createDID();
        CredOfferStructure off1= store.StoreIndy.returnCredentialOffer(SpedizioneAnonimaCredDef.credDefId);
        CredRequestStructure crs1=receiver.returnproverCreateCredentialRequest(off1);
        CreateCredentialResult ccr1=store.StoreIndy.returnIssuerCreateCredentialNonRevocable
                (new String[]{"SerialNumber","Destinazione"},new String[]{"SerialNumber","Destinazione"},off1.credOffer,crs1.credReqJson);
        */
        //System.out.println("ccr1"+ccr1.credentialJson);

        //avvio Thread che gestisce clienti


        SchemaStructure SpedizioneAnonimaSchema =store.createPackageCredentialSchemaAndPublish(
                "customershippingschema","2.0",new String[]{"shippingid","diddest","lockerboxid",
                "itemname","storedid","shippingnonce"});



        CredDefStructure SpedizioneAnonimaCredDef = store.createPackageCredentialDefinitionAndPublish
                ("tag",false,SpedizioneAnonimaSchema.schemaId);

        store.openStoreToClients();
        System.out.println("store aperto");


    }

}
