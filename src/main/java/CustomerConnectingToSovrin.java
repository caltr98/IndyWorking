import IndyLibraries.*;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.pool.Pool;
import org.json.JSONObject;

import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class CustomerConnectingToSovrin {
    public static void main(String[] args) throws IOException {
        System.out.println("Insert a known store DID");
        Scanner sc = new Scanner(System.in);
        String storeDID=sc.next();
        Pool pool= null;
        try {
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
        } catch (
                IndyException | IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        JSONUserCredentialStorage jsonStoredCred=null;
        File agentsFile=new File("./"+"agentsFile"+".json");
        try {
            jsonStoredCred = new JSONUserCredentialStorage(agentsFile);
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Agent agentCustomer = new Agent(pool,"cliente7",jsonStoredCred);
        agentCustomer.CreateWallet("walletCliente206","abcd");
        agentCustomer.OpenWallet("walletCliente206","abcd");
        agentCustomer.createDID();
        //open comunication with store


        String endpointSteward = agentCustomer.getEndPointFromLedger(storeDID);
        String[] componentsOfEndpoint= endpointSteward.split(":");
        String endpoitTransportKey = agentCustomer.getDIDVerKeyFromLedger(storeDID);//it is the DID verkey
        SocketChannel connectionTOStore =SocketChannel.open();
        connectionTOStore.connect(new InetSocketAddress(componentsOfEndpoint[0],
                Integer.parseInt(componentsOfEndpoint[1])));

        String storeDID2DID = agentCustomer.createConnection(connectionTOStore.socket(),"cliente");

        //richiesta Items verso Store

        JSONObject request = new JSONObject();
        request.put("request","Cred_Request_0");

        agentCustomer.sendD2DMessage(agentCustomer.askForCredentialMSG(storeDID2DID),
                connectionTOStore.socket());
        String response=new String((agentCustomer.waitForMessage(connectionTOStore.socket())),
                Charset.defaultCharset());
        System.out.println("Response \n"+
                response);


        //parse response to get cred def, cred offer and array of items
        JSONObject responseJOBJ = new JSONObject(response);
        JSONObject cred_def_OBJ = new JSONObject(responseJOBJ.getString("credDef"));
        String myDID2DID = responseJOBJ.getString("requesterDID");
        //client needs to recreate CredOffer and CredDef since the store sends more information than just the cred. off.
        CredDefStructure credDefStructure =
                new CredDefStructure(responseJOBJ.getString("credDefId"),cred_def_OBJ.toString(4));
        CredOfferStructure credOfferReceived = new CredOfferStructure(credDefStructure,
                responseJOBJ.getString("credOffer"));


        //BEFORE

        agentCustomer.createMasterSecret("3b80a521-cf9d-4076-b29c-91741254a665");
        CredRequestStructure cred_req=agentCustomer.returnproverCreateCredentialRequest(credOfferReceived);
        System.out.println("request format \n"+
                cred_req.credReqJson);

        String selectedItem=itemSelect(responseJOBJ.getJSONArray("Items").toList(),sc);

        //EXTRA OP:user orders an Item and Store will reserve it for him
        JSONObject jsonObjectItemOrder = new JSONObject();
        jsonObjectItemOrder.put("request","Order_Item");
        jsonObjectItemOrder.put("itemSelection",selectedItem);

        byte[]orderMessage=agentCustomer.writeMessage(jsonObjectItemOrder.toString(),storeDID2DID);
        agentCustomer.sendD2DMessage(orderMessage, connectionTOStore.socket());

        String itemOrderResponse=new String(agentCustomer.waitForMessage(connectionTOStore.socket()),
                Charset.defaultCharset());
        System.out.println(itemOrderResponse);


        System.out.println("Customer Credential Request size in bytes ->"+ cred_req.credReqJson.getBytes(StandardCharsets.UTF_8).length+" bytes");

        byte[] credential_req=agentCustomer.credentialRequestMSG(storeDID2DID,cred_req.credReqJson,credOfferReceived.credOffer);
        System.out.println("Credential RequestMSG to Send to the Store size in bytes->"+ credential_req.length+" bytes");
        agentCustomer.sendD2DMessage(credential_req,connectionTOStore.socket());
        JSONObject credentialResponse=new JSONObject(new String(agentCustomer.waitForMessage(connectionTOStore.socket()),
                Charset.defaultCharset()));
        System.out.println("Credential"+credentialResponse.getString("credential"));
        System.out.println("Received Credentials for Item in LockerBox @address:"+credentialResponse.getString("BoxAddress"));
        String storeCredential=agentCustomer.storeCredentialInWallet(null,credDefStructure.credDefId,cred_req.credReqMetadataJson,
                credentialResponse.getString("credential"),
                credDefStructure.credDefJson,
                null);

        //ask to box for item! client search the Lockerbox identified by the lockerbox attribute in the credential
        System.out.println("Insert Lockerbox port number ,address is (127.0.0.1)");
        int portn=sc.nextInt();

        Customer2Box box = new Customer2Box(agentCustomer,"127.0.0.1",
                portn,"anonCustomer");
        String itemFromBoxResult=box.getItemFromBOX();
        boolean stop=false;
        while( itemFromBoxResult==null && !stop){
            System.out.println("Wrong Box to ask for Item OR Box is Empty because Item has not arrived yet" +
                    " , want to ask another box or the same box y/n?");
            if(sc.next().equals("y")){
                System.out.println("Insert Lockerbox port number ,address is (127.0.0.1)");
                portn=sc.nextInt();
                box = new Customer2Box(agentCustomer,"127.0.0.1",
                        portn,"anonCustomer");
                itemFromBoxResult=box.getItemFromBOX();
            }
            else{
                stop=true;
                agentCustomer.proverDeleteCredential(itemFromBoxResult);//delete the credential
                //identified by the credential id returned by getItemFromBox method

            }
        }
        System.out.println("CUSTOMER METRICS after getting Item:\n"+agentCustomer.collectMetrics());
        System.out.println("SUCESS!! Item "+selectedItem+" collected");
    }


    private static String itemSelect(List<Object> itemsList, Scanner sc){
        int i;
        if (itemsList.size()==0){
            System.out.println("No Items Avaible :(");
            return null;
        }
        System.out.println("Avaible Items");
        for (i = 0; i < itemsList.size() ; i++) {
            System.out.println(i+")"+" - "+itemsList.get(i));
        }
        System.out.println("Select Item Number");
        i=sc.nextInt();
        if(i<itemsList.size() && i>=0){
            return (String)itemsList.get(i);
        }
        return null;
    }
}
