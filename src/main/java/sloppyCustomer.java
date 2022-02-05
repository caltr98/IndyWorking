import IndyLibraries.*;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.pool.Pool;
import org.json.JSONObject;

import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class sloppyCustomer extends CustomerMarket {

    public static void main(String[] args) throws IOException {
        System.out.println("Insert a known store DID");
        Scanner sc = new Scanner(System.in);
        String storeDID=sc.next();
        Pool pool= null;
        try {
            Pool.setProtocolVersion(2).get();
            pool = Pool.openPoolLedger("INDYSCANPOOL", "{}").get();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
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
        JSONObject megarequest = new JSONObject();
        megarequest.put("request","Cred_Request_1");
        megarequest.put("credReq",cred_req.credReqJson);
        megarequest.put("credOffer",credOfferReceived.credOffer);
        megarequest.put("credReqMetadata",cred_req.credReqMetadataJson);
        byte[] credential_req=agentCustomer.writeMessage(megarequest.toString(4),storeDID2DID);
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
