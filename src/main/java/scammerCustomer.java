import IndyLibraries.*;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.pool.Pool;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.NoSuchPaddingException;
import java.io.File;
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

public class scammerCustomer {
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

        ScammerCustomer2Box box = new ScammerCustomer2Box(agentCustomer,"127.0.0.1",
                portn,"anonCustomer");
        String itemFromBoxResult=box.getItemFromBOX();
        boolean stop=false;
        while( itemFromBoxResult==null && !stop){
            System.out.println("Wrong Box to ask for Item OR Box is Empty because Item has not arrived yet" +
                    " , want to ask another box or the same box y/n?");
            if(sc.next().equals("y")){
                System.out.println("Insert Lockerbox port number ,address is (127.0.0.1)");
                portn=sc.nextInt();
                box = new ScammerCustomer2Box(agentCustomer,"127.0.0.1",
                        portn,"anonCustomer");
                itemFromBoxResult=box.getItemFromBOX();
            }
            else{
                stop=true;
                //agentCustomer.proverDeleteCredential(itemFromBoxResult);//delete the credential
                //identified by the credential id returned by getItemFromBox method

            }
        }
        //System.out.println("CUSTOMER METRICS after getting Item:\n"+agentCustomer.collectMetrics());
        System.out.println("SUCESS!! Item "+selectedItem+" collected");
        System.out.println("Give me the shippingId of the next Delivery");
        String newShippingId=sc.next();
        System.out.println("Give me the shippingNonce of the next Delivery");
        String newShippingnonce =sc.next();
        box.adjustProofValueToScam(newShippingId,newShippingnonce);
        box.askForConnectionToBox();
        System.out.println("result from scam? "+box.stealItemFromBOX());


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
class ScammerCustomer2Box{
    Agent customerIndy;
    String box2StoreDID;
    InetSocketAddress boxAddress;
    DatagramSocket ds;
    String customerName;
    ProofCreationExtra proof;

    public ScammerCustomer2Box(Agent customerIndy, String boxHostname, int boxHostPort, String customerName) {
        this.boxAddress = null;
        this.customerIndy = customerIndy;
        boxAddress = new InetSocketAddress(boxHostname, boxHostPort);
        ds = null;
        this.customerName = customerName;
        try {
            ds = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        askForConnectionToBox();
    }

    public void askForConnectionToBox(){
        try {
            ds = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        box2StoreDID = customerIndy.createDID2DIDCommunication(customerName, boxAddress,this.ds);
    }

    public String stealItemFromBOX(){
        JSONObject messageJOBJECT= new JSONObject();
        messageJOBJECT.put("request","GetItem0");
        //sending request for object
        byte [] msg=customerIndy.writeMessage(messageJOBJECT.toString(),box2StoreDID);
        sendMSG(msg,this.boxAddress);
        //receiving proof request
        String proofRequest = receiveMSG(ds);
        if(proofRequest.equals("Box is empty")){
            System.out.println("Box is empty, customer arrived too late to collect item");
            return "WrongTime";
        }
        else{
            System.out.println("Received proof request "+proofRequest);
            JSONObject proofreqstructure = new JSONObject(proofRequest);
            String proofReq= proofreqstructure.getString("proofrequest");
            //Using adjusted proof to steal item
            JSONObject sendProof= new JSONObject().put("proof",proof.proofJson).put("request","GetItem1").put("cred_defs",proof.credentialDefs)
                    .put("schemas",proof.schemas).put("proofrequest",proofReq);

            msg = customerIndy.writeMessage(sendProof.toString(4), box2StoreDID);
            System.out.println("Message to Send to LockerBox encrypted and with proof additional information"+ msg.length+" bytes");
            sendMSG(msg,this.boxAddress);
            String response=receiveMSG(ds);
            boolean resultcheck = response.equals("Success");
            if(resultcheck || response.equals("WrongTime")){
                //if item is received correctly then customer deletes the credential relative to it
                //(also deletes it if customer arrives late to get the package)
                System.out.println("Item Stealed ");
            }
            else{
                System.out.println("Wrong Box for the given credential");
                response=null;
            }
            return response;
        }
    }
    public String getItemFromBOX(){
        JSONObject messageJOBJECT= new JSONObject();
        messageJOBJECT.put("request","GetItem0");
        //sending request for object
        byte [] msg=customerIndy.writeMessage(messageJOBJECT.toString(),box2StoreDID);
        sendMSG(msg,this.boxAddress);
        //receiving proof request
        String proofRequest = receiveMSG(ds);
        if(proofRequest.equals("Box is empty")){
            System.out.println("Box is empty, customer arrived too late to collect item");
            return "WrongTime";
        }
        else{
            System.out.println("Received proof request "+proofRequest);
            JSONObject proofreqstructure = new JSONObject(proofRequest);
            String proofReq= proofreqstructure.getString("proofrequest");
            ArrayList<String> requested_attributes=new ArrayList<String>(Arrays.asList(proofreqstructure.
                    getJSONArray("requested_revealed_attributes").toList().
                    toArray(String[]::new)));
            ProofAttributesFetched attributesFetched=
                    customerIndy.
                            returnProverSearchAttrForProof
                                    (proofReq,  requested_attributes);
            System.out.println("client credential fetch for proof metrics:\n"+customerIndy.collectMetrics());

            //String selfAttestedOpeningTime= String.valueOf(System.currentTimeMillis());
            proof=customerIndy.proverCreateProof(attributesFetched,proofReq, null
                    ,null,-1);

            System.out.println("Customer! Created Proof Size in Bytes-> "+proof.proofJson.getBytes(StandardCharsets.UTF_8).length+" bytes");

            //ProofCreationExtra proof= customerIndy.returnProverSearchAttrAndCreateProofSimple(proofReq,null,currentTimeofproof);
            //NOTE:Proof with predicates is really big, a device with little memory can't handle it
            System.out.println("Proof Dimension"+proof.proofJson.getBytes(StandardCharsets.UTF_8).length+" bytes");
            JSONObject sendProof= new JSONObject().put("proof",proof.proofJson).put("request","GetItem1").put("cred_defs",proof.credentialDefs)
                    .put("schemas",proof.schemas).put("proofrequest",proof.proofRequestJson);

            msg = customerIndy.writeMessage(sendProof.toString(4), box2StoreDID);
            System.out.println("Message to Send to LockerBox encrypted and with proof additional information"+ msg.length+" bytes");
            sendMSG(msg,this.boxAddress);
            String response=receiveMSG(ds);
            boolean resultcheck = response.equals("Success");
            if(resultcheck || response.equals("WrongTime")){
                //if item is received correctly then customer deletes the credential relative to it
                //(also deletes it if customer arrives late to get the package)
                System.out.println("Item received correctly, i'll keep the credential, just in case...");
            }
            else{
                System.out.println("Wrong Box for the given credential");
                response=null;
            }
            return response;
        }
    }

    public boolean adjustProofValueToScam(String shippingId,String shippingNonce){
        System.out.println(
                proof.proofJson);
        JSONObject changeProof = new JSONObject(proof.proofJson);
        System.out.println("changeproof"+ changeProof.toString(4));
        JSONObject modifyfields = changeProof.getJSONObject("requested_proof");

        System.out.println("beforechanges\n"+modifyfields.toString(4));

        modifyfields.put("revealed_attr_groups",new JSONObject().put("attr0_referent",(new JSONObject().put("values",new JSONObject().
                        put("shippingid",new JSONObject().put("raw",shippingId).put("encoded",shippingId)).
                        put("shippingnonce",new JSONObject().put("raw",shippingNonce).put("encoded",shippingNonce))
                        )).put("sub_proof_index",0)));
        System.out.println("afterchanges\n"+modifyfields.toString(4));
        changeProof.put("requested_proof",modifyfields);
        System.out.println("fullNEWPROOF\n"+changeProof.toString(4));

        JSONArray modifyfieldsArray = changeProof.getJSONObject("proof").getJSONArray("proofs");
        JSONObject modifyFields2 = modifyfieldsArray.getJSONObject(0);
        modifyfields = modifyFields2.getJSONObject("primary_proof");
        JSONObject modifyfields3= modifyfields.getJSONObject("eq_proof");
        modifyfields3.put("revealed_attrs",new JSONObject().
                put("shippingid",shippingId).
                put("shippingnonce",shippingNonce));
        modifyfields.put("eq_proof",modifyfields3);
        modifyFields2.put("primary_proof",modifyfields);
        modifyfieldsArray.put(0,modifyFields2);
        changeProof = changeProof.put("proof",changeProof.getJSONObject("proof").put("proofs",modifyfieldsArray));
        System.out.println("change"+changeProof.toString(4));
        this.proof.proofJson=changeProof.toString(4);
        return true;
    }
    private void sendMSG(String msg,InetSocketAddress theirAddress) {
        byte[] toSend = msg.getBytes();
        try  {

            DatagramPacket datagramPacket = new DatagramPacket(toSend, toSend.length,
                    theirAddress.getAddress(),theirAddress.getPort());
            if(datagramPacket.getLength()>ds.getSendBufferSize()) {
                //there is the need to make a tcp connection to lockerbox for sending a big proof
                this.sendMSG("TCP_CONNECTION_REQUEST", theirAddress);
                DatagramPacket receiver=new DatagramPacket(new byte[65535],65536);
                ds.receive(receiver);
                JSONObject toConnectAddress = new JSONObject(new String(receiver.getData(),
                        Charset.defaultCharset()));
                try (Socket socket1 = new Socket()) {//automatically closes the socket
                    socket1.bind(null);
                    socket1.connect(new InetSocketAddress(toConnectAddress.getString("ip"),
                            Integer.parseInt(toConnectAddress.getString("port"))));
                    //send all the data
                    socket1.getOutputStream().write(toSend);
                }
            }
            else {
                this.ds.send(datagramPacket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    protected void sendMSG(byte[] toSend, InetSocketAddress theirAddress) {
        try  {
            DatagramPacket datagramPacket = new DatagramPacket(toSend, toSend.length,
                    theirAddress.getAddress(),theirAddress.getPort());
            if(datagramPacket.getLength()>ds.getSendBufferSize()){
                //there is the need to make a tcp connection to lockerbox for sending a big proof
                this.sendMSG("TCP_CONNECTION_REQUEST", theirAddress);
                DatagramPacket receiver=new DatagramPacket(new byte[65535],65535);
                ds.receive(receiver);
                JSONObject toConnectAddress = new JSONObject(new String(receiver.getData(),
                        Charset.defaultCharset()));
                try (Socket socket1 = new Socket()) {//automatically closes the socket
                    socket1.bind(null);
                    socket1.connect(new InetSocketAddress(toConnectAddress.getString("ip"),
                            (toConnectAddress.getInt("port"))));
                    //send all the data
                    socket1.getOutputStream().write(toSend);
                }
            }
            else {
                this.ds.send(datagramPacket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    protected String receiveMSG(DatagramSocket dsock){
        byte[]datagramBuffer=new byte[65535];
        String message;
        DatagramPacket toReceive = new DatagramPacket(datagramBuffer, 65535);
        try {
            dsock.receive(toReceive);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int i;
        byte [] dataArray = new byte[toReceive.getLength()];
        for(i= 0 ;i <toReceive.getLength();i++ ){
            dataArray[i]=datagramBuffer[i];
        }
        message = customerIndy.readMessage(dataArray);
        return message;
    }

}


