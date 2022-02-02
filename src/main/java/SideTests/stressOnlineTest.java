package SideTests;

import IndyLibraries.Endorser;
import IndyLibraries.IndyJsonStringBuilder;
import IndyLibraries.JSONUserCredentialStorage;
import IndyLibraries.SchemaStructure;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.anoncreds.Anoncreds;
import org.hyperledger.indy.sdk.anoncreds.AnoncredsResults;
import org.hyperledger.indy.sdk.did.Did;
import org.hyperledger.indy.sdk.ledger.Ledger;
import org.hyperledger.indy.sdk.pool.Pool;
import org.json.JSONObject;

import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class stressOnlineTest {
    //Similar to sideTestIndy but this time Spotify/External service issues credentials too.
    public static void main(String[] args) {
        Pool pool = null;
        File agentsFile = new File("./agentsFile.json");
        JSONUserCredentialStorage jsonStoredCred = null;
        //University DID: L8m6zGD1RLXKD4hbYorhXe University VerKey:BRriQz9X2YiD2aDgyfPfnKnHzPeqhCVMbYbZ1U7YVGPV

        try {
            jsonStoredCred = new JSONUserCredentialStorage(agentsFile);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
        try {
            Pool.setProtocolVersion(2).get();

            //1. Create and Open Pool
            //poolName = IndyLibraries.PoolUtils.createPoolLedgerConfig();
            JSONObject j = new JSONObject();
            String data = ("{\"reqSignature\":{},\"txn\":{\"data\":{\"data\":{\"alias\":\"FoundationBuilder\",\"blskey\":\"3gmhmqpPLqznZF3g3niodaHjbpsB6TEeE9SpgXgBnZJLmXgeRzJqTLajVwbhxrkomJFTFU4ohDC4ZRXKbUPCQywJuPAQnst8XBtCFredMECn4Z3goi1mNt5QVRdU8Ue2xMSkdLpsQMjCsNwYUsBguwXYUQnDXQXnHqRkK9qrivucQ5Z\",\"blskey_pop\":\"RHWacPhUNc9JWsGNdmWYHrAvvhsow399x3ttNKKLDpz9GkxxnTKxtiZqarkx4uP5ByTwF4kM8nZddFKWuzoKizVLttALQ2Sc2BNJfRzzUZMNeQSnESkKZ7U5vE2NhUDff6pjANczrrDAXd12AjSG61QADWdg8CVciZFYtEGmKepwzP\",\"client_ip\":\"35.161.146.16\",\"client_port\":\"9702\",\"node_ip\":\"50.112.53.5\",\"node_port\":\"9701\",\"services\":[\"VALIDATOR\"]},\"dest\":\"GVvdyd7Y6hsBEy5yDDHjqkXgH8zW34K74RsxUiUCZDCE\"},\"metadata\":{\"from\":\"V5qJo72nMeF7x3ci8Zv2WP\"},\"type\":\"0\"},\"txnMetadata\":{\"seqNo\":1,\"txnId\":\"fe991cd590fff10f596bb6fe2362229de47d49dd50748e38b96f368152be29c7\"},\"ver\":\"1\"}\n" +
                    "{\"reqSignature\":{},\"txn\":{\"data\":{\"data\":{\"alias\":\"vnode1\",\"blskey\":\"t5jtREu8au2dwFwtH6QWopmTGxu6qmJ3iSnk321yLgeu7mHQRXf2ZCBuez8KCAQvFZGqqAoy2FcYvDGCqQxRCz9qXKgiBtykzxjDjYu87JECwwddnktz5UabPfZmfu6EoDn4rFxvd4myPu2hksb5Z9GT6UeoEYi7Ub3yLFQ3xxaQXc\",\"blskey_pop\":\"QuHB7tiuFBPQ6zPkwHfMtjzWqXJBLACtfggm7zCRHHgdva18VN4tNg7LUU2FfKGQSLZz1M7oRxhhgJkZLL19aGvaHB2MPtnBWK9Hr8LMiwi95UjX3TVXJri4EvPjQ6UUvHrjZGUFvKQphPyVTMZBJwfkpGAGhpbTQuQpEH7f56m1X5\",\"client_ip\":\"206.189.143.34\",\"client_port\":\"9796\",\"node_ip\":\"206.189.143.34\",\"node_port\":\"9797\",\"services\":[\"VALIDATOR\"]},\"dest\":\"9Aj2LjQ2fwszJRSdZqg53q5e6ayScmtpeZyPGgKDswT8\"},\"metadata\":{\"from\":\"FzAaV9Waa1DccDa72qwg13\"},\"type\":\"0\"},\"txnMetadata\":{\"seqNo\":2,\"txnId\":\"5afc282bf9a7a5e3674c09ee48e54d73d129aa86aa226691b042e56ff9eaf59b\"},\"ver\":\"1\"}\n" +
                    "{\"reqSignature\":{},\"txn\":{\"data\":{\"data\":{\"alias\":\"xsvalidatorec2irl\",\"blskey\":\"4ge1yEvjdcV6sDSqbevqPRWq72SgkZqLqfavBXC4LxnYh4QHFpHkrwzMNjpVefvhn1cgejHayXTfTE2Fhpu1grZreUajV36T6sT4BiewAisdEw59mjMxkp9teYDYLQqwPUFPgaGKDbFCUBEaNdAP4E8Q4UFiF13Qo5842pAY13mKC23\",\"blskey_pop\":\"R5PoEfWvni5BKvy7EbUbwFMQrsgcuzuU1ksxfvySH6FC5jpmisvcHMdVNik6LMvAeSdt6K4sTLrqnaaQCf5aCHkeTcQRgDVR7oFYgyZCkF953m4kSwUM9QHzqWZP89C6GkBx6VPuL1RgPahuBHDJHHiK73xLaEJzzFZtZZxwoWYABH\",\"client_ip\":\"52.50.114.133\",\"client_port\":\"9702\",\"node_ip\":\"52.209.6.196\",\"node_port\":\"9701\",\"services\":[\"VALIDATOR\"]},\"dest\":\"DXn8PUYKZZkq8gC7CZ2PqwECzUs2bpxYiA5TWgoYARa7\"},\"metadata\":{\"from\":\"QuCBjYx4CbGCiMcoqQg1y\"},\"type\":\"0\"},\"txnMetadata\":{\"seqNo\":3,\"txnId\":\"1972fce7af84b7f63b7f0c00495a84425cce3b0c552008576e7996524cca04cb\"},\"ver\":\"1\"}\n" +
                    "{\"reqSignature\":{},\"txn\":{\"data\":{\"data\":{\"alias\":\"danube\",\"blskey\":\"3Vt8fxn7xg8n8pR872cvGWNuR7STFzFSPMftX96zF6871wYVTR27aspxGSeEtx9wj8g4D3GdCxHJbQ4FsxQz6TATQswiiZfxAVNjLLUci8WSH4t1GPx9CvGXB2uzDfVnnJyhhnASxJEbvykLUBBFG3fW4tMQixujpowUADz5jHm427u\",\"blskey_pop\":\"RJpXXLkjRRv9Lk8tJz8LTkhhC7RWjHQcB9CG8J8U8fXT6arTDMYc62zXtToBAmGkGu8Udsmo3Hh7mv4KB9JAf8ufGY9WsnppCVwar7zEXyBfLpCnDhvVcBAzkhRpHmqHygN24DeBu9aH6tw4uXxVJvRRGSbPtxjWa379BmfQWzXHCb\",\"client_ip\":\"207.180.207.73\",\"client_port\":\"9702\",\"node_ip\":\"173.249.14.196\",\"node_port\":\"9701\",\"services\":[\"VALIDATOR\"]},\"dest\":\"52muwfE7EjTGDKxiQCYWr58D8BcrgyKVjhHgRQdaLiMw\"},\"metadata\":{\"from\":\"VbPQNHsvoLZdaNU7fTBeFx\"},\"type\":\"0\"},\"txnMetadata\":{\"seqNo\":4,\"txnId\":\"ebf340b317c044d970fcd0ca018d8903726fa70c8d8854752cd65e29d443686c\"},\"ver\":\"1\"}\n"
            );
            File f = new File("./pool_transactions_builder_genesis");
            new FileOutputStream(f).write(data.getBytes(StandardCharsets.UTF_8));
            j.put("genesis_txn", "./pool_transactions_builder_genesis");
            Pool.createPoolLedgerConfig("builder", j.toString());
            pool = Pool.openPoolLedger("builder", "{}").get();
        } catch (InterruptedException | ExecutionException | IndyException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            jsonStoredCred = new JSONUserCredentialStorage(agentsFile);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Endorser university = new Endorser(pool, "university", jsonStoredCred);
        //University DID: L8m6zGD1RLXKD4hbYorhXe University VerKey:BRriQz9X2YiD2aDgyfPfnKnHzPeqhCVMbYbZ1U7YVGPV
        university.OpenWallet("endorserwallet", "abcd");
        university.createDID("4392f283a046fce4afce8c3b2c503104a7f48bb44bae7192fbf767df7ecfff92");
        try {
            Did.createAndStoreMyDid(university.mainWallet,"4392f283a046fce4afce8c3b2c503104a7f48bb44bae7192fbf767df7ecfff92");
        } catch (IndyException e) {
            e.printStackTrace();
        }
        AnoncredsResults.IssuerCreateSchemaResult createSchemaResult =
                null;
        String schemaId;
        String schemaJson;
        SchemaStructure schemaStructure;
        long accepttime=1643160836; //must be in range.
        String taa="8cee5d7a573e4893b08ff53a0761a22a1607df3b3fcd7e75b98696c92879641f";
                String acceptance="for_session";
        try {
            System.out.println("Do you agree to perform 20 transactions?");
            Scanner sc= new Scanner(System.in);
            sc.next();
            String[] attributesForSchema = {"lot","of","attributes","abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789" ,"1"};
            //Null data or empty JSON are acceptable here. In this case, ledger will return the latest version of TAA.
            int i;
            /*test sulle cred schema errate
            for (i=0; i<20; i++) {
                university.publishschemaOnSovrin("testschemaNumber", String.valueOf(i),attributesForSchema,taa,acceptance,accepttime);
            }*/
            //test sulle cred schema corrette
            /*for (i=0; i<20; i++) {
                university.publishschemaOnSovrin("testschemaNumbem", String.valueOf(i)+".0",attributesForSchema,taa,acceptance,accepttime);
            }*//*
            //get schema + cred def on non existing schema
            /*
            schemaJson="{\"ver\":\"1.0\",\"id\":\"31s62mrw7S2waAbga7n8n9:2:testschemaNumber:50.0\",\"name\":\"testschemaNumber\",\"version\":\"50.0\",\"attrNames\":[\"1\",\"of\",\"llibutes\",\"re\",\"lot\"],\"seqNo\":1}";
            for (i=0;i<20;i++){
                university.IssuerCreateStoreAndPublishPrefinedSchemaCredDefOnSovrin("c*"+i,true,"31s62mrw7S2waAbga7n8n9:2:testschemaNumber:50.0",taa,acceptance,accepttime,schemaJson);
            }
            */

            schemaJson="{\"ver\":\"1.0\",\"id\":\"31s62mrw7S2waAbga7n8n9:2:testschemaNumber:18.0\",\"name\":\"testschemaNumber\",\"version\":\"18.0\",\"attrNames\":[\"1\",\"of\",\"attributes\",\"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789\",\"lot\"],\"seqNo\":15222}";
            university.IssuerCreateStoreAndPublishPrefinedSchemaCredDefOnSovrin("X",true,"31s62mrw7S2waAbga7n8n9:2:testschemaNumber:18.0",taa,acceptance,accepttime,schemaJson);



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
