import IndyLibraries.*;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.pool.Pool;
import org.json.JSONObject;

import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class ShippingAgent implements Runnable {
    private String shippingAgentName;
    private Agent IndyShippingAgent;
    private Pool pool;
    private Long deliveryIdentifier;
    private HashMap<SocketAddress,String>addresstodid;
    private InetSocketAddress currentTalker;
    private DatagramSocket datagramSocket;
    private HashMap<Long,DeliveryObject> requestedDeliveries;
    private Queue<DeliveryObject> deliveries;
    public ShippingAgent(String  poolName,String shippingAgentName,String walletName,String walletpass){
        this.requestedDeliveries= new HashMap<>();
        this.deliveries = new LinkedList<>();
        this.shippingAgentName=shippingAgentName;
        this.addresstodid=new HashMap<>();
        this.currentTalker =null;
        try {
            this.datagramSocket=new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        try {
            Pool.setProtocolVersion(2).get();
            pool = Pool.openPoolLedger(poolName, "{}").get();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.pool = pool;
        JSONUserCredentialStorage jsonStoredCred=null;
        File agentsFile=new File("./"+"agentsFile"+".json");
        try {
            jsonStoredCred = new JSONUserCredentialStorage(agentsFile);
        }catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        IndyShippingAgent= new Agent(pool,shippingAgentName,jsonStoredCred);
        IndyShippingAgent.CreateWallet("shippingAgentWallet"+shippingAgentName,"abcd");
        IndyShippingAgent.OpenWallet("shippingAgentWallet"+shippingAgentName,"abcd");
        IndyShippingAgent.createDID();
        IndyShippingAgent.createMasterSecret();

    }
    @Override
    public void run() {
        DatagramSocket datagramSocket=this.datagramSocket;
        byte[]msg;
        String message;
        boolean stop=false;
        String receivedMSG,handled;
        System.out.println("Shipping Agent is ready for conenction @ "+ datagramSocket.getLocalSocketAddress());
        while (!stop) {
            while (!deliveries.isEmpty()){
                deliveries.remove().deliverItem();
                System.out.println("Shipping AGent metrics after one delivery\n"+IndyShippingAgent.collectMetrics());
            }
            receivedMSG=receiveMSG(datagramSocket);
            //integra handshake in handle message
            if(receivedMSG!=null){
                handled=handleMessage(receivedMSG);
                System.out.println(handled);
            }
        }
    }
    private String receiveMSG(DatagramSocket dsock){
        byte[]datagramBuffer=new byte[65535];
        String didToDIDofIP,message;
        DatagramPacket toReceive = new DatagramPacket(datagramBuffer, 65535);
        try {
            dsock.receive(toReceive);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String did = addresstodid.get(new InetSocketAddress(toReceive.getAddress(),toReceive.getPort()));
        byte [] dataArray;
        if(did == null){
            System.out.println("no did address guy"+toReceive.getAddress());
            int i;
            dataArray = new byte[toReceive.getLength()];
            for(i= 0 ;i <toReceive.getLength();i++ ){
                dataArray[i]=datagramBuffer[i];
            }
            String didToDIDInfo=IndyShippingAgent.acceptConnection
                    (new String(dataArray,Charset.defaultCharset()),
                            this.shippingAgentName);
            JSONObject jsonObjectInfoForDID2DID = new JSONObject(didToDIDInfo);
            didToDIDInfo= jsonObjectInfoForDID2DID.getString("message");
            didToDIDofIP = jsonObjectInfoForDID2DID.getString("theirDID");
            if(didToDIDInfo.equals("Wrong Fields")||didToDIDofIP.equals("Illegal response format"))
                return null;
            addresstodid.put(new InetSocketAddress(toReceive.getAddress(),toReceive.getPort()),didToDIDofIP);
            sendMSG(didToDIDInfo,new InetSocketAddress(toReceive.getAddress(),toReceive.getPort()));
            message=null;
        }else {
            int i;
            dataArray = new byte[toReceive.getLength()];
            for(i= 0 ;i <toReceive.getLength();i++ ){
                dataArray[i] = datagramBuffer[i];
            }
            if(new String(dataArray,Charset.defaultCharset()).equals("TCP_CONNECTION_REQUEST")){
                try(ServerSocket socket1 =new ServerSocket()) {
                    socket1.bind(null);
                    int port=socket1.getLocalPort();
                    //String address=socket1.getLocalAddress().getHostAddress();
                    String address="127.0.0.1";
                    JSONObject toSendInfoForConnection= new JSONObject();
                    toSendInfoForConnection.put("ip",address);
                    toSendInfoForConnection.put("port",port);
                    sendMSG(toSendInfoForConnection.toString(),this.currentTalker);
                    dataArray=socket1.accept().getInputStream().readAllBytes();//read all bytes until other side closes
                    // connection
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            message = IndyShippingAgent.readMessage(dataArray);
            currentTalker = new InetSocketAddress(toReceive.getAddress(),toReceive.getPort());
            System.out.println("arrived message from store!" + message);
        }
        return message;
    }

    private String handleMessage(String message){
        System.out.println("RECEIVED MES"+ message);
        JSONObject messageJOBJECT= new JSONObject(message);
        if(messageJOBJECT.get("request").equals("GetShippingAgentID")) {
            byte[] msg = IndyShippingAgent.writeMessage(this.shippingAgentName,
                    addresstodid.get(currentTalker));
            sendMSG(msg, currentTalker);
            return "Success giving ShippingAgentID";
        }

        if(messageJOBJECT.get("request").equals("HandleDelivery0")){
            //add this delivery in the list of delivery to handle
            DeliveryObject deliveryObject =new DeliveryObject(
                        messageJOBJECT.getString("itemCredentialOffer"));
                this.requestedDeliveries.put(deliveryObject.deliveryIdentifier,deliveryObject);
                byte [] msg= IndyShippingAgent.writeMessage(new JSONObject().put
                        ("deliveryId",deliveryObject.deliveryIdentifier).toString(),addresstodid.get(currentTalker));
                sendMSG(msg, currentTalker);
                msg=deliveryObject.DeliveryCredRequest();
            sendMSG(msg, currentTalker);
            return "Success";
            }
        if(messageJOBJECT.get("request").equals("HandleDelivery1")){
            String credential,boxEndpointAddr;
            int boxEndpointPort;
            deliveryIdentifier = messageJOBJECT.getLong("deliveryId");
            if(this.requestedDeliveries.containsKey(deliveryIdentifier)) {
                credential = messageJOBJECT.getString("Credential");
                //String boxEndpointAddr = messageJOBJECT.get("LockerBoxAgentIP"); for non-localhost use
                boxEndpointAddr = "127.0.0.1";// for localhost test
                boxEndpointPort = messageJOBJECT.getInt("LockerBoxAgentPortNo");
                System.out.println("box port number"+ boxEndpointPort);
                DeliveryObject currdeliveryObject=requestedDeliveries.remove(deliveryIdentifier);
                if(currdeliveryObject.DeliveryCred(credential,boxEndpointAddr,boxEndpointPort).equals("Success")){
                    System.out.println("delivery is beeing processed");
                    deliveries.add(currdeliveryObject);
                    return "Success";
                }
                return "Fail";
            }
        }
        return "Wrong Message";
    }

    private void sendMSG(String msg,InetSocketAddress theirAddress) {
        byte[] toSend = msg.getBytes();
        try  {
            DatagramPacket datagramPacket = new DatagramPacket(toSend, toSend.length,
                    theirAddress.getAddress(),theirAddress.getPort());
            this.datagramSocket.send(datagramPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendMSG(byte[] toSend,InetSocketAddress theirAddress) {
        try  {
            DatagramPacket datagramPacket = new DatagramPacket(toSend, toSend.length,
                    theirAddress.getAddress(),theirAddress.getPort());
            this.datagramSocket.send(datagramPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private class DeliveryObject {
        private String credOffer;
        CredOfferStructure DeliveryCredentialOffer;
        int lockerBoxPort;
        String lockerBoxAddress;
        CredRequestStructure DeliveryCredentialRequest;
        long deliveryIdentifier;
        InetSocketAddress boxAddress;
        private DatagramSocket ds;
        private String box2ShippingAgentDID;

        public DeliveryObject(
                              String credOffer){
            this.credOffer = credOffer;
            try {
                this.ds=new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            }

            deliveryIdentifier=System.currentTimeMillis();
        }
        public byte[] DeliveryCredRequest(){
            JSONObject cred_offerJson = new JSONObject(this.credOffer);
            JSONObject cred_def=new JSONObject(cred_offerJson.getString("credDef"));
            //client needs to recreate CredOffer and CredDef since the store sends more information than just the cred. off.
            CredDefStructure credDefStructure =
                    new CredDefStructure(cred_offerJson.getString("credDefId"),cred_def.toString(4));
            CredOfferStructure credOfferReceived = new CredOfferStructure(credDefStructure,
                    cred_offerJson.getString("credOffer"));
            this.DeliveryCredentialOffer = credOfferReceived;
            CredRequestStructure cred_req=IndyShippingAgent.returnproverCreateCredentialRequest(DeliveryCredentialOffer);
            this.DeliveryCredentialRequest= cred_req;
            System.out.println("CREDENTIAL REQUEST"+ cred_req.credReqJson);
            return IndyShippingAgent.credentialRequestMSG(addresstodid.get(currentTalker),this.DeliveryCredentialRequest.credReqJson,
                    this.DeliveryCredentialOffer.credOffer);

        }
        public String DeliveryCred(String credential,String LockerBoxAddress, int LockerBoxPort){


            String storeCredential=IndyShippingAgent.storeCredentialInWallet(null,DeliveryCredentialOffer.credDef.credDefId
                    ,DeliveryCredentialRequest.credReqMetadataJson,
                    credential,
                    DeliveryCredentialOffer.credDef.credDefJson,
                    null);
            this.lockerBoxAddress=LockerBoxAddress;
            this.lockerBoxPort=LockerBoxPort;
            return "Success";

        }

        public  String openBox(){
            //TO DO implement a method that fetch credential and searches for a specific credential to
            //use in the proof!
            return null;
        }
        public String deliverItem() {
            //handshake with LockerBox and ask to insert Item in Box
            boxAddress = new InetSocketAddress(this.lockerBoxAddress, this.lockerBoxPort);
            box2ShippingAgentDID = IndyShippingAgent.createDID2DIDCommunication("anon",boxAddress,ds);
            addresstodid.put(boxAddress,box2ShippingAgentDID);
            JSONObject messageJOBJECT = new JSONObject().put("request", "PutItem0");
            //sending request for object
            byte[] msg = IndyShippingAgent.writeMessage(messageJOBJECT.toString(), box2ShippingAgentDID);
            sendMSG(msg, this.boxAddress);
            //receiving proof request
            String proofRequest = receiveMSG(this.ds);

            if (proofRequest.equals("Box was not Booked for delivery")) {
                System.out.println("Box was not Booked for delivery");
            } else {
                JSONObject proofreqstructure = new JSONObject(proofRequest);
                String proofReq = proofreqstructure.getString("proofrequest");
                ArrayList<String> requested_revealed = new ArrayList<String>(Arrays.asList(proofreqstructure.
                        getJSONArray("requested_revealed_attributes").toList().
                        toArray(String[]::new)));
                ProofAttributesFetched attributesFetched =
                        IndyShippingAgent.
                                returnProverSearchAttrForProof
                                        (proofReq, requested_revealed);

                ProofCreationExtra proof = IndyShippingAgent.proverCreateProof(attributesFetched, proofReq,null
                        , null, -1);
                //ProofCreationExtra proof= customerIndy.returnProverSearchAttrAndCreateProofSimple(proofReq,null,currentTimeofproof);
                //NOTE:Proof with predicates is really big, a device with little memory can't handle it
                System.out.println("Proof Dimension" + proof.proofJson.getBytes(StandardCharsets.UTF_8).length + " bytes");
                JSONObject sendProof = new JSONObject().put("proof", proof.proofJson).put("request", "PutItem1").put("cred_defs", proof.credentialDefs)
                        .put("schemas", proof.schemas).put("proofrequest", proof.proofRequestJson);
                msg = IndyShippingAgent.writeMessage(sendProof.toString(4),
                        box2ShippingAgentDID);
                System.out.println("D2D proof message size (with cred_defs and schemas)" + msg.length + " bytes");
                sendMSG(msg, this.boxAddress);
                String response = receiveMSG(this.ds);
                return response;
            }
            return proofRequest;
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
            message = IndyShippingAgent.readMessage(dataArray);
            return message;
        }


    }

}
