import IndyLibraries.*;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.pool.Pool;
import org.json.JSONObject;

import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class LockerBoxAgent implements Runnable {
    private Boolean isReserved;
    private Long timestampcanopen;
    private Long timestampreturnitem;
    private String boxName;
    private Agent IndyLockerBox;
    private Pool pool;
    private boolean stop;
    private HashMap<SocketAddress,String>addresstodid;
    private String storedid;
    private String shippingcreddefid;
    private String shippingId;
    private String lastProofRequestToCustomer;
    private InetSocketAddress currentTalker;
    private String shippingNonce;
    private String lastProofRequestToShippingAgent;
    private String shipmentcreddefid;
    private String shipmentNonce;
    private String shipmentId;
    private boolean isOccupied;

    public LockerBoxAgent(String  poolName,String boxname){
        //box setup if it doesn't have a public DID
        this.boxName=boxname;
        this.stop=false;
        this.isReserved =false;
        this.isOccupied = false;
        this.timestampreturnitem=Long.MAX_VALUE;
        this.timestampcanopen= Long.MIN_VALUE;
        this.addresstodid=new HashMap<>();
        this.currentTalker =null;
        try {
            Pool.setProtocolVersion(2).get();
            this.pool = Pool.openPoolLedger(poolName, "{}").get();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.boxName=boxname;
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
        IndyLockerBox= new Endorser(pool,boxName,jsonStoredCred);

        IndyLockerBox.CreateWallet("boxWallet"+boxname,"abcd");
        IndyLockerBox.OpenWallet("boxWallet"+boxname,"abcd");
        IndyLockerBox.createDID();
    }
    public LockerBoxAgent(String  poolName,String boxname,String didValue,String walletName,String walletpass){
        //box setup if it doesn't have a public DID
        this.boxName=boxname;
        this.stop=false;
        this.isReserved =false;
        this.timestampreturnitem=null;
        this.timestampcanopen=null;
        this.addresstodid=new HashMap<>();
        this.currentTalker =null;
        this.timestampreturnitem=Long.MAX_VALUE;
        this.timestampcanopen= Long.MIN_VALUE;

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
        this.boxName=boxname;
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

        IndyLockerBox= new Endorser(pool,boxName,jsonStoredCred);
        IndyLockerBox.CreateWallet("boxWallet"+boxname,"abcd");
        IndyLockerBox.OpenWallet("boxWallet"+boxname,"abcd");

        IndyLockerBox.getStoredDIDandVerkey(didValue);

    }

    public LockerBoxAgent(Pool pool, String boxname) {
        //box setup with a given pool handle!
        //can be used to connect to sovrin!
        this.boxName=boxname;
        this.stop=false;
        this.isReserved =false;
        this.isOccupied = false;
        this.timestampreturnitem=Long.MAX_VALUE;
        this.timestampcanopen= Long.MIN_VALUE;
        this.addresstodid=new HashMap<>();
        this.currentTalker =null;
        this.pool=pool;
        this.boxName=boxname;
        JSONUserCredentialStorage jsonStoredCred=null;
        File agentsFile=new File("./"+"agentsFile"+".json");
        try {
            jsonStoredCred = new JSONUserCredentialStorage(agentsFile);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
        IndyLockerBox= new Endorser(pool,boxName,jsonStoredCred);
        IndyLockerBox.CreateWallet("boxWallet"+boxname,"abcd");
        IndyLockerBox.OpenWallet("boxWallet"+boxname,"abcd");
        IndyLockerBox.createDID();
    }

    private void occupyBOX(String shippingcreddefid, String shippingId,String shippingNonce,
                           String shipmentcreddefid, String shipmentId,String shipmentNonce,String storedid){
        this.shippingcreddefid =shippingcreddefid;
        this.shippingId =shippingId;
        this.shippingNonce=shippingNonce;

        this.shipmentcreddefid =shipmentcreddefid;
        this.shipmentId =shipmentId;
        this.shipmentNonce=shipmentNonce;

        this.isReserved =true;
        this.storedid=storedid;
    }
    private void givePackageToClient(){}

    private void returnPackageToSender(){

    }

    @Override
    public void run() {
        DatagramSocket datagramSocket=null;
        byte[]msg;
        String message;
        try {
            datagramSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        String receivedMSG,handled;

        System.out.println("Box is ready for conenction @ "+ datagramSocket.getLocalSocketAddress());
        while (!stop) {

            receivedMSG=receiveMSG(datagramSocket);
            //integra handshake in handle message
            if(receivedMSG!=null){
                handled=handleMessage(receivedMSG);
                System.out.println(handled);
            }
        }
    }
    private String handleMessage(String message){
        System.out.println("RECEIVED MES"+ message);
        JSONObject messageJOBJECT= new JSONObject(message);
        if(messageJOBJECT.get("request").equals("GetBoxID")) {
            byte[] msg = IndyLockerBox.writeMessage(this.boxName, addresstodid.get(currentTalker));
            sendMSG(msg, currentTalker);
            return "Success giving BoxId";
        }
        if(messageJOBJECT.get("request").equals("InsertItem")){
            if(!isReserved) {


                occupyBOX(messageJOBJECT.getString("shippingcreddefid")
                ,messageJOBJECT.getString("shippingid"),
                        messageJOBJECT.getString("shippingnonce"),

                        messageJOBJECT.getString("shipmentcredefid"),
                        messageJOBJECT.getString("shipmentid"),
                        messageJOBJECT.getString("shipmentnonce"),
                        messageJOBJECT.getString("storedid"));
                byte [] msg=IndyLockerBox.writeMessage("Success",addresstodid.get(currentTalker));
                sendMSG(msg, currentTalker);

                return "Success";
            }
            else {
                byte [] msg=IndyLockerBox.writeMessage("Box already full",addresstodid.get(currentTalker));
                sendMSG(msg, currentTalker);

                return "Box already full";
            }
        }
        if(messageJOBJECT.get("request").equals("PutItem0")){ //shipping agent inserting Item in booked by store lockerbox
            if(isReserved) { // box must be booked by store
                JSONObject proofRequestStucture= new JSONObject();
                this.lastProofRequestToShippingAgent = askShippingAgentForProofRequest();
                proofRequestStucture.put("proofrequest",this.lastProofRequestToShippingAgent);
                //request attributes to be reavealed cant be done in the  proof request,
                //it is part of  the communication protocol beetween shippingagent and client agent to ask shipmentAvailabilityTime
                //from credential, so that lockerbox is sure that the value is the one decided by the Store
                proofRequestStucture.put("requested_revealed_attributes",new String[]
                        {"attr0_referent"});
                String proofReq= proofRequestStucture.toString(4);

                byte [] msg=IndyLockerBox.writeMessage(proofReq,addresstodid.get(currentTalker));
                sendMSG(msg, currentTalker);
                return proofReq;
            }
            else {
                byte [] msg=IndyLockerBox.writeMessage("Box was not Booked for delivery",addresstodid.get(currentTalker));
                sendMSG(msg, currentTalker);

                return "Box was not Booked for delivery";
            }
        }
        if(messageJOBJECT.get("request").equals("PutItem1")){ //shipping agent inserting Item in booked by store lockerbox
            String proofReceived  = messageJOBJECT.getString("proof");
            //NOTE: schemas and cred defs must be the same returned by the prover
            String proofrequestreceived= messageJOBJECT.getString("proofrequest");
            String schemasReceived =messageJOBJECT.getString("schemas");
            String creddefsReceived=messageJOBJECT.getString("cred_defs");
            System.out.println("receivedProof"+ proofReceived);
            if(this.lastProofRequestToShippingAgent ==null){
                byte [] msg=IndyLockerBox.writeMessage("request item first",
                        addresstodid.get(currentTalker));
                sendMSG(msg, currentTalker);
                return "failure";
            }
            boolean verifyRes=
                    IndyLockerBox.returnVerifierVerifyProofNOREVOCATION(this.lastProofRequestToShippingAgent,proofReceived,
                            schemasReceived,creddefsReceived);
            System.out.println("Is credential Valid?:"+verifyRes+"\n");

            if(verifyRes) {
                //if proof is valid then box takes from proof the time of availability and
                //starts a timer for the time the customer can get the item

                byte[] msg=null;
                String itemInsert=insertItemInBox(proofReceived);
                if(itemInsert.equals("Success")){
                    msg = IndyLockerBox.writeMessage("Success", addresstodid.get(currentTalker));
                    this.isReserved = false;
                    sendMSG(msg, currentTalker);
                    this.isOccupied=true;
                    startTimer();
                    return "Success proof is : \n"+proofReceived;
                }
            }
            else{
                byte[] msg = IndyLockerBox.writeMessage("Failure, proof is not valid", addresstodid.get(currentTalker));
                sendMSG(msg, currentTalker);
                this.isReserved = true;
            }
            return "Failure";


        }


        if(messageJOBJECT.get("request").equals("GetItem0")){
            if(!isOccupied){
                byte [] msg=IndyLockerBox.writeMessage("Box is empty",addresstodid.get(currentTalker));
                sendMSG(msg, currentTalker);

                return "Box is empty";
            }
            this.lastProofRequestToCustomer =askClientForProofRequest();
            JSONObject proofRequestStucture= new JSONObject();
            proofRequestStucture.put("proofrequest",this.lastProofRequestToCustomer);
            //request attributes to be reavealed cant be done in the  proof request,
            //it is part of  the communication protocol beetween lockerboxagent and client agent to ask for them
            //and they are needed to open the box even if the proof is valid
            proofRequestStucture.put("requested_revealed_attributes",new String[]
                    {"attr0_referent"});
            String proofReq= proofRequestStucture.toString(4);

            byte [] msg=IndyLockerBox.writeMessage(proofReq,addresstodid.get(currentTalker));
            sendMSG(msg, currentTalker);
            return proofReq;
        }
        if(messageJOBJECT.getString("request").equals("GetItem1")){
            String proofReceived  = messageJOBJECT.getString("proof");
            String proofrequestreceived= messageJOBJECT.getString("proofrequest");
            String schemasReceived =messageJOBJECT.getString("schemas");
            String creddefsReceived=messageJOBJECT.getString("cred_defs");
            System.out.println("receivedProof"+ proofReceived);
            if(this.lastProofRequestToCustomer ==null){
                byte [] msg=IndyLockerBox.writeMessage("request item first",
                        addresstodid.get(currentTalker));
                sendMSG(msg, currentTalker);
                return "failure";
            }
            boolean verifyRes=
            IndyLockerBox.returnVerifierVerifyProofNOREVOCATION(this.lastProofRequestToCustomer,proofReceived,schemasReceived,creddefsReceived);
            System.out.println("Is credential Valid?:"+verifyRes+"\n");
            System.out.println("LockerBox after  open to customer metrics:\n"+IndyLockerBox.collectMetrics());

            if(verifyRes) {
                //CHECK openingTime is within the allowed time limits
                byte[] msg=null;
                long currentOpeningTime = System.currentTimeMillis();
                String openBoxResult="validproof";
                if(currentOpeningTime>=this.timestampcanopen && currentOpeningTime<=this.timestampreturnitem){
                    openBoxResult="Success";
                }
                else{
                    openBoxResult="WrongTime";
                }
                if(openBoxResult.equals("Success")){
                    msg = IndyLockerBox.writeMessage("Success", addresstodid.get(currentTalker));
                    this.isOccupied = false ;
                    sendMSG(msg, currentTalker);
                    return "Success proof is : \n"+proofReceived;
                }/*cannot happen given current restrictions
                else if(openBoxResult.equals("InvalidProof")){
                    //neger
                    msg = IndyLockerBox.writeMessage("WrongItem", addresstodid.get(currentTalker));
                    sendMSG(msg, currentTalker);
                    return "Failure proof is valid but not for the current Item";
                }*/
                else{
                    msg = IndyLockerBox.writeMessage("WrongTime", addresstodid.get(currentTalker));
                    sendMSG(msg, currentTalker);
                    return "Failure proof is valid but Customer arrived too late";

                }
            }
            else{
                byte[] msg = IndyLockerBox.writeMessage("Failure, proof is not valid", addresstodid.get(currentTalker));
                sendMSG(msg, currentTalker);
                this.isReserved =true;
            }
            return "Failure";
        }
        return "Wrong Message";
    }

    private void startTimer() {
        //timer can be implemented with a thread that wakes up and sends a proof request

        //mock method -> 1 in 10 possibility of timer elapisng
        Random r = new Random();
        int i = r.nextInt(9) + 1;
        if(i == 10){
            timestampreturnitem = Long.valueOf(0); //check will always return false in handleMessage().
        }
    }

    private String insertItemInBox(String proofReceived) {
        Long currentTime;
        JSONObject proofStructure = new JSONObject(proofReceived);
        JSONObject revealed_attr=(proofStructure.getJSONObject("requested_proof").getJSONObject("revealed_attr_groups").getJSONObject("attr0_referent")
                .getJSONObject("values"));
        Long proofOpeningBoxTime = Long.valueOf(revealed_attr.getJSONObject("shipmentavailabilitytime").getString("raw"));
        currentTime = System.currentTimeMillis();
        if (proofOpeningBoxTime>0){
            this.timestampcanopen =currentTime;
            this.timestampreturnitem = currentTime+ TimeUnit.HOURS.toMillis(proofOpeningBoxTime);
            this.isReserved= true;
            return "Success";
        }
        return "WrongTime";
    }

    private void sendMSG(String msg,InetSocketAddress theirAddress) {
        byte[] toSend = msg.getBytes();
        try (DatagramSocket sendSocket = new DatagramSocket()) {
            DatagramPacket datagramPacket = new DatagramPacket(toSend, toSend.length,
                    theirAddress.getAddress(),theirAddress.getPort());
            sendSocket.send(datagramPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendMSG(byte[] toSend,InetSocketAddress theirAddress) {
        try (DatagramSocket sendSocket = new DatagramSocket()) {
            DatagramPacket datagramPacket = new DatagramPacket(toSend, toSend.length,
                    theirAddress.getAddress(),theirAddress.getPort());
            sendSocket.send(datagramPacket);
        } catch (IOException e) {
            e.printStackTrace();
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
            int i;
             dataArray = new byte[toReceive.getLength()];
            for(i= 0 ;i <toReceive.getLength();i++ ){
                dataArray[i]=datagramBuffer[i];
            }
            String didToDIDInfo=IndyLockerBox.acceptConnection
                    (new String(dataArray,Charset.defaultCharset()),
                            this.boxName);
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
            message = IndyLockerBox.readMessage(dataArray);
            currentTalker = new InetSocketAddress(toReceive.getAddress(),toReceive.getPort());
            System.out.println("arrived message from store!" + message);
        }
        return message;
    }
    private String askClientForProofRequest(){
        //send proof request to client!
        //the proof request is designed such that the client needs to have the shippingid and diddest designed for the
        //current item in the box, it is done by using Indy predicates
        //the proof request also requests the LockerBoxID and the ItemName but those attributes can be also unrevealed
        //(LockerBox does not need to know its contents)
        Long currenttime=null;
        int [] arr;
        JSONObject []arrayofAttrreq = new JSONObject[4];
        //NOTE: the attribute shippingid,lockerboxid,shippingnoce attributes must be  equals to those
        // of the current idem,equality will be checked with restrition :
        // "attr::attr_name::value": "requested_value", attribute dont need to be in a revealed form to
        // check for equality ( it protects the customer so that if he sends a wrong credential
        // the lockerbox will not know he has the credential for another item shipping )
        System.out.println("shippingId" + this.shippingId + "shipping nonce "+shippingNonce );
        arrayofAttrreq[0] = IndyLockerBox.generateAttrInfoForProofRequest(null,
                new String[]{"shippingnonce","shippingid"},
                null,null,null,null,
                this.storedid,this.shippingcreddefid,new String[]{"shippingnonce","shippingid"},
                new String[]{this.shippingNonce,this.shippingId},null,currenttime,currenttime);


        System.out.println("attrinfo - request - structure\n"+arrayofAttrreq[0].toString(4));
        String proofReqbody= IndyLockerBox.returnVerifierGenerateProofRequest("GetItemProof"+String.valueOf(System.currentTimeMillis())
                ,"1.0","1.0",
                arrayofAttrreq,null,currenttime,currenttime);

        System.out.println("proof - request - Body\n"+proofReqbody);
        System.out.println("LockerBox to Customer proof request size"+ proofReqbody.length());
        return proofReqbody;
    }

    //send proof request to shipping agent!
    //the proof request is designed such that the client needs to have the shippingid and shipping created for the item,
    //as for the customer it is done by way of equality check of attributes in the proof thanks to the restriction
    //attr::attr_name::value
    private String askShippingAgentForProofRequest(){
        Long currenttime=null;
        int [] arr;
        JSONObject []arrayofAttrreq = new JSONObject[4];
        //NOTE: the attribute shippingid,lockerboxid,shippingnoce attributes must be  equals to those
        // of the current idem,equality will be checked with restrition :
        // "attr::attr_name::value": "requested_value", attribute dont need to be in a revealed form to

        arrayofAttrreq[0] = IndyLockerBox.generateAttrInfoForProofRequest(null,
                new String[]{"shipmentavailabilitytime","shipmentnonce","shipmentid"},
                null,null,null,null,
                this.storedid,this.shipmentcreddefid,new String[]{"shipmentnonce","shipmentid"},
                new String[]{this.shipmentNonce,this.shipmentId},null,currenttime,currenttime);


        String proofReqbody= IndyLockerBox.returnVerifierGenerateProofRequest("GetItemProof"+String.valueOf(System.currentTimeMillis())
                ,"1.0","1.0",
                arrayofAttrreq,null,currenttime,currenttime);
        System.out.println("Shipment agent attrinfo - request - structure\n"+arrayofAttrreq[0].toString(4));
        return proofReqbody;
    }



    /*
    private String askClientForProofRequest(){
        //send proof request to client!
        //the proof request is designed such that the client needs to have the shippingid and diddest designed for the
        //current item in the box, it is done by using Indy predicates
        //the proof request also requests the LockerBoxID and the ItemName but those attributes can be also unrevealed
        //(LockerBox does not need to know its contents)
        Long currenttime=null;
        int [] arr;
        JSONObject []arrayofAttrreq = new JSONObject[4];
        //NOTE: the attribute shippingid,lockerboxid,shippingnoce attributes must be  revealed to check if it is the correct credential
        // for the current idem.
        arrayofAttrreq[0] = IndyLockerBox.generateAttrInfoForProofRequest(null,
                new String[]{"lockerboxid","shippingnonce"},
        null,null,null,null,
                this.storedid,this.creddefid,new String[]{"lockerboxid","shippingnonce"},
        new String []{this.boxName,this.shippingNonce},null,currenttime,currenttime);

        //the lockerbox also also time of opening must be given by the customer, time of opening is
        //a self attested attribute that must also be revealed and will be used to check that the
        //customer is opening the box in the allowed time
        arrayofAttrreq[1] =IndyLockerBox.generateAttrInfoForProofRequest("openingTime",
                null,null,null,null,
                null,null,null, null, null, null,currenttime,currenttime);

        //those predicates check are not a 100% correct proof because the customer could present an
        // attribute that makes the predicates true but even if client owns a shipping_id1>=currentBOXshipping_id
        // and shipping_id2<=currentBOXshipping it is not guranteed that shipping_id1 == shipping_id2
        // equality CANT BE ENFORCED

        JSONObject []arrayofPredReq = new JSONObject[2];
        //Predicated: shippingid>= currentItemShippingId, it should loosely prove that the customer has the current shipped
        //item credential, loosely because a client could have a credential with a shippingid>=itemshippingid and <=itemshippingid
        //the predicate will be a preliminary check before checking the revealed attribute shippingnonce
        //if predicates are false then LockerBox does not need to check the revealed attribute shippingnoce

        //NOTE:in the ClientAgent shippingid will be equal to itemshippingid because the agent will throw away
        //any used credential and will request only one item at a time

        //since predicateType are only  "p_type": predicate type (">=", ">", "<=", "<")
        arrayofAttrreq[2]= IndyLockerBox.generatePredicatesInfoForProofRequest("shippingid",">=",this.shippingId,null,null,null,null,
                this.storedid,this.creddefid,null,currenttime,currenttime);
        arrayofAttrreq[= IndyLockerBox.generatePredicatesInfoForProofRequest("shippingid","<=",this.shippingId,null,null,null,null,
                this.storedid,this.creddefid,null,currenttime,currenttime);

        String proofReqbody= IndyLockerBox.returnVerifierGenerateProofRequest("GetItemProof"+String.valueOf(System.currentTimeMillis())
                ,"1.0","1.0",
                arrayofAttrreq,arrayofPredReq,currenttime,currenttime);
        return proofReqbody;
    }*/

    /* Old version without restrictions  attr::shippingnonce::value
    public String openBox(String proofToGetAttributes){
        JSONObject proofStructure = new JSONObject(proofToGetAttributes);
        JSONObject revealed_attr=(proofStructure.getJSONObject("requested_proof").getJSONObject("revealed_attr_groups").getJSONObject("attr0_referent")
                .getJSONObject("values"));
        String proofLockerBoxId= revealed_attr.getJSONObject("lockerboxid").getString("raw");
        String proofShippingnonce = revealed_attr.getJSONObject("shippingnonce").getString("raw");

        Long proofOpeningBoxTime = Long.valueOf(proofStructure.getJSONObject("requested_proof").getJSONObject("self_attested_attrs").
                getString("attr1_referent"));

        boolean shippingNonceIsCorrect= this.shippingNonce.equals(proofShippingnonce);
        boolean proofLockerBoxIdIsCorrect= this.boxName.equals(proofLockerBoxId);
        boolean openingTimeisValid = proofOpeningBoxTime>=this.timestampcanopen && proofOpeningBoxTime<=this.timestampreturnitem;
        System.out.println(shippingNonce + " " +proofShippingnonce);
        System.out.println(this.boxName +  " "+proofLockerBoxId);
        System.out.println(timestampcanopen +"  "+proofOpeningBoxTime+"  "+  timestampreturnitem);
        return   shippingNonceIsCorrect && proofLockerBoxIdIsCorrect ?
                (openingTimeisValid ? "Success" : "WrongTime") : "InvalidProof";
    }*/


    /*This method would ask for each attribute individually, meaning that they could have come from
    differents credentials (even valid ones if issued by the right issuer, the correct methods
    uses the field names:[string,string] in proof request, for asking more attributes belonging to the same
    credential!

    private String askClientForProofRequest(){
        //send proof request to client!
        //the proof request is designed such that the client needs to have the shippingid and diddest designed for the
        //current item in the box, it is done by using Indy predicates
        //the proof request also requests the LockerBoxID and the ItemName but those attributes can be also unrevealed
        //(LockerBox does not need to know its contents)
        Long currenttime=null;
        int [] arr;
        JSONObject []arrayofAttrreq = new JSONObject[4];
        arrayofAttrreq[0] = IndyLockerBox.generateAttrInfoForProofRequest("lockerboxid",null,null,null,null,null,
                this.storedid,this.creddefid,null,currenttime,currenttime);
        arrayofAttrreq[1] = IndyLockerBox.generateAttrInfoForProofRequest("itemname",null,null,null,null,null,
                this.storedid,this.creddefid,null,currenttime,currenttime);

        arrayofAttrreq[2] = IndyLockerBox.generateAttrInfoForProofRequest("diddest",null,null,null,null,null,

                this.storedid,this.creddefid,null,currenttime,currenttime);

        arrayofAttrreq[3] = IndyLockerBox.generateAttrInfoForProofRequest("storedid",null,null,null,null,null,
                this.storedid,this.creddefid,null,currenttime,currenttime);



        JSONObject []arrayofPredReq = new JSONObject[4];
        //since predicateType are only  "p_type": predicate type (">=", ">", "<=", "<")
        //to enforce equality it is needed to creare a request with pred.type '>=' and  another with '<='
        arrayofPredReq[0]= IndyLockerBox.generatePredicatesInfoForProofRequest("shippingid",">=",this.shippingId,null,null,null,null,
                this.storedid,this.creddefid,null,currenttime,currenttime);
        arrayofPredReq[1]= IndyLockerBox.generatePredicatesInfoForProofRequest("shippingid","<=",this.shippingId,null,null,null,null,
                this.storedid,this.creddefid,null,currenttime,currenttime);

        arrayofPredReq[2] = IndyLockerBox.generatePredicatesInfoForProofRequest("shippingnonce",">=",this.shippingNonce,null,null,null,null,
                this.storedid,this.creddefid,null,currenttime,currenttime);
        arrayofPredReq[3] = IndyLockerBox.generatePredicatesInfoForProofRequest("shippingnonce","<=",this.shippingNonce,null,null,null,null,
                this.storedid,this.creddefid,null,currenttime,currenttime);

       String proofReqbody= IndyLockerBox.returnVerifierGenerateProofRequest("GetItemProof"+String.valueOf(System.currentTimeMillis())
                ,"1.0","1.0",
                arrayofAttrreq,arrayofPredReq,currenttime,currenttime);
        return proofReqbody;
    }*/
}
