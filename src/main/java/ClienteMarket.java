import IndyLibraries.*;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.pool.Pool;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.NoSuchPaddingException;
import javax.xml.crypto.Data;
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

public class ClienteMarket {
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
        Agent agentCliente = new Agent(pool,"cliente7",jsonStoredCred);
        agentCliente.CreateWallet("walletCliente206","abcd");
        agentCliente.OpenWallet("walletCliente206","abcd");
        agentCliente.createDID();
        //open comunication with store


        String endpointSteward = agentCliente.getEndPointFromLedger(storeDID);
        String[] componentsOfEndpoint= endpointSteward.split(":");
        String endpoitTransportKey = agentCliente.getDIDVerKeyFromLedger(storeDID);//it is the DID verkey
        SocketChannel connectionTOStore =SocketChannel.open();
        connectionTOStore.connect(new InetSocketAddress(componentsOfEndpoint[0],
                Integer.parseInt(componentsOfEndpoint[1])));

        String storeDID2DID = agentCliente.createConnection(connectionTOStore.socket(),"cliente");

        //richiesta Items verso Store

        JSONObject request = new JSONObject();
        request.put("request","Cred_Request_0");

        agentCliente.sendD2DMessage(agentCliente.askForCredentialMSG(storeDID2DID),
                connectionTOStore.socket());
        String response=new String((agentCliente.waitForMessage(connectionTOStore.socket())),
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

        System.out.println("master secret"+agentCliente.createMasterSecret());
        agentCliente.createMasterSecret("3b80a521-cf9d-4076-b29c-91741254a665");
        CredRequestStructure cred_req=agentCliente.returnproverCreateCredentialRequest(credOfferReceived);
        System.out.println("request format \n"+
                cred_req.credReqJson);

        String selectedItem=itemSelect(responseJOBJ.getJSONArray("Items").toList(),sc);

        //EXTRA OP:user orders an Item and Store will reserve it for him
        JSONObject jsonObjectItemOrder = new JSONObject();
        jsonObjectItemOrder.put("request","Order_Item");
        jsonObjectItemOrder.put("itemSelection",selectedItem);

        byte[]orderMessage=agentCliente.writeMessage(jsonObjectItemOrder.toString(),storeDID2DID);
        agentCliente.sendD2DMessage(orderMessage, connectionTOStore.socket());
        String itemOrderResponse=new String(agentCliente.waitForMessage(connectionTOStore.socket()),
                Charset.defaultCharset());
        System.out.println(itemOrderResponse);

        byte[] credential_req=agentCliente.credentialRequestMSG(storeDID2DID,cred_req.credReqJson,credOfferReceived.credOffer);

        agentCliente.sendD2DMessage(credential_req,connectionTOStore.socket());
        JSONObject credentialResponse=new JSONObject(new String(agentCliente.waitForMessage(connectionTOStore.socket()),
                Charset.defaultCharset()));
        System.out.println("Credential"+credentialResponse.getString("credential"));

        String storeCredential=agentCliente.storeCredentialInWallet(null,credDefStructure.credDefId,cred_req.credReqMetadataJson,
                credentialResponse.getString("credential"),
                credDefStructure.credDefJson,
                null);
        System.out.println("store"+storeCredential);

        //ask to box for item! client search the Lockerbox identified by the lockerbox attribute in the credential
        System.out.println("Insert Lockerbox port number ,address is (127.0.0.1)");
        int portn=sc.nextInt();

        Customer2Box box = new Customer2Box(agentCliente,"127.0.0.1",
                portn,"anonCustomer");
        String itemFromBoxResult=box.getItemFromBOX();
        boolean stop=false;
        while( !( itemFromBoxResult.equals("Success") ||
                itemFromBoxResult.equals("WrongTime") ) && !stop){
            System.out.println("Wrong Box to ask for Item OR Box is Empty because Item has not arrived yet" +
                    " , want to ask another box or the same box y/n?");
            if(sc.next().equals("y")){
                System.out.println("Insert Lockerbox port number ,address is (127.0.0.1)");
                portn=sc.nextInt();
                box = new Customer2Box(agentCliente,"127.0.0.1",
                        portn,"anonCustomer");
                itemFromBoxResult=box.getItemFromBOX();
            }
            else{
                stop=true;
                agentCliente.proverDeleteCredential(itemFromBoxResult);//delete the credential
                //identified by the credential id returned by getItemFromBox method
            }
        }
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
class Customer2Box {
    Agent customerIndy;
    String box2StoreDID;
    InetSocketAddress boxAddress;
    DatagramSocket ds;
    String customerName;

    public Customer2Box(Agent customerIndy, String boxHostname, int boxHostPort,
                        String customerName) {
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
        box2StoreDID = customerIndy.createDID2DIDComunication(customerName, boxAddress,this.ds);
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
            System.out.println("LA PROOF REQUEST ARRIVATA!"+proofRequest);
            JSONObject proofreqstructure = new JSONObject(proofRequest);
            String proofReq= proofreqstructure.getString("proofrequest");
            ArrayList<String> requested_revealed=new ArrayList<String>(Arrays.asList(proofreqstructure.
                    getJSONArray("requested_revealed_attributes").toList().
                    toArray(String[]::new)));
            ProofAttributesFetched attributesFetched=
                    customerIndy.
                            returnProverSearchAttrForProof
                                    (proofReq,  requested_revealed);

            String selfAttestedOpeningTime= String.valueOf(System.currentTimeMillis());
            ProofCreationExtra proof=customerIndy.proverCreateProof(attributesFetched,proofReq,
                    new String[]{selfAttestedOpeningTime}, null
                    ,null,-1);

            //ProofCreationExtra proof= customerIndy.returnProverSearchAttrAndCreateProofSimple(proofReq,null,currentTimeofproof);
            //NOTE:Proof with predicates is really big, a device with little memory can't handle it
            System.out.println("Proof Dimension"+proof.proofJson.getBytes(StandardCharsets.UTF_8).length+" bytes");
            JSONObject sendProof= new JSONObject().put("proof",proof.proofJson).put("request","GetItem1").put("cred_defs",proof.credentialDefs)
                    .put("schemas",proof.schemas).put("proofrequest",proof.proofRequestJson);
            msg = customerIndy.writeMessage(sendProof.toString(4), box2StoreDID);
            System.out.println("D2D proof message size (with cred_defs and schemas)"+ msg.length+" bytes");
            sendMSG(msg,this.boxAddress);
            String response=receiveMSG(ds);
            boolean resultcheck = response.equals("Success");
            if(resultcheck || response.equals("WrongTime")){
                //if item is received correctly then customer deletes the credential relative to it
                //(also deletes it if customer arrives late to get the package)
                customerIndy.proverDeleteCredential(attributesFetched.credentialID.get(0));
            }
            else{
                System.out.println("Wrong Box for the given credential");
                response=attributesFetched.credentialID.get(0);//return the credential_id
                //in the case the customer decides not to get the item from another box
            }
            return response;
        }
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
    private void sendMSG(byte[] toSend,InetSocketAddress theirAddress) {
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


    private String receiveMSG(DatagramSocket dsock){
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

