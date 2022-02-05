import IndyLibraries.CredDefStructure;
import IndyLibraries.SchemaStructure;

import java.util.Scanner;

public class SloppyStoreMainTest {
    public static void main(String[] args) {
        System.out.println("Create a new Store? y/n or default?");
        Scanner sc = new Scanner(System.in);
        SloppyStore store;
        String itemToInsert, storename, storewalletpass, storeDID;
        String a = sc.next();
        if (a.equals("y")) {
            System.out.println("insert storeName");
            storename = sc.next();
            System.out.println("insert store DID wallet password");
            storewalletpass = sc.next();
            store = new SloppyStore("INDYSCANPOOL", storename, storewalletpass); //Create a new Store identity on Ledger
        } else if (a.equals("n")) {
            System.out.println("insert store DID value (remember to save it)");
            storeDID = sc.next();
            System.out.println("insert storeName");
            storename = sc.next();
            System.out.println("insert store DID wallet password");
            storewalletpass = sc.next();
            store = new SloppyStore("INDYSCANPOOL", storename, storeDID, storewalletpass); //Create a new Store identity on Ledger

        } else {
            //NOTE:Default must be setup first
            store = new SloppyStore("INDYSCANPOOL",
                    "nomeStore123", "YRDrcLTz9CB4Ly8PbtLB2i", "abcd"); //insert an already created store did and wallet pass here
        }

        store.addItem("Penna");
        System.out.println("Write X to stop adding Items to Store or Insert new Item name");
        while (!((itemToInsert = sc.next()).equals("X"))) {
            store.addItem(itemToInsert);
        }

        SchemaStructure SpedizioneAnonimaSchema = store.createPackageCredentialSchemaAndPublish(
                "customershippingschema1", "2.0", new String[]{"shippingid", "diddest", "lockerboxid",
                        "itemname", "storedid", "shippingnonce"});


        CredDefStructure SpedizioneAnonimaCredDef = store.createPackageCredentialDefinitionAndPublish
                ("tag1", false, SpedizioneAnonimaSchema.schemaId);

        SchemaStructure gestioneSpedizioneSchema = store.createShippingCredentialSchemaAndPublish(
                "shipmentparcel1", "1.0", new String[]{"shipmentid", "shipmentweight", "shipmentdeclaredvalue"
                        , "shipmentnonce", "shipmentavailabilitytime"});


        CredDefStructure gestioneSpedizioneCredDef = store.createShippingCredentialDefinitionAndPublish(
                "shippingclaim2", false, gestioneSpedizioneSchema.schemaId);


        store.openStoreToClients();
        System.out.println("store is open");


    }
}
