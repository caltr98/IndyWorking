import IndyLibraries.*;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.pool.Pool;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.NoSuchPaddingException;
import javax.xml.crypto.Data;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Store {
    String storeName;
    ServerSocketChannel AgentListenChannel;
    InetSocketAddress listeningAddress;
    ConcurrentHashMap<String,Item> ItemsCollection;//ItemName->Item
    Endorser StoreIndy;
    Thread listeningThread;
    Integer shippingid;
    Pool pool;
    CredRequestStructure preFetchedREQ;
    CredDefStructure packageCredential;
    SchemaStructure packageSchema;
    CredOfferStructure offerToClient;
    ConcurrentHashMap<String,String> sendingItemTo;
    ArrayList <Store2Box> boxes = new ArrayList<>();
    public Store(String poolName,String storeName,String stewardDID){
        //creating the store, making public in the Ledger his DID and starting initializing his
        //endpoint for starting DID2DID comm

        try {
            Pool pool;
            Pool.setProtocolVersion(2).get();
            pool = Pool.openPoolLedger(poolName, "{}").get();
            this.pool = pool;
            this.storeName=storeName;
            this.shippingid = 1;
            JSONUserCredentialStorage jsonStoredCred=null;
            File agentsFile=new File("./"+"agentsFile"+".json");
            sendingItemTo  = new ConcurrentHashMap<>();
            jsonStoredCred= new JSONUserCredentialStorage(agentsFile);
            StoreIndy = new Endorser(pool,storeName,jsonStoredCred);
            StoreIndy.CreateWallet("storeWallet1"+this.storeName,"abcd");
            StoreIndy.OpenWallet("storeWallet1"+this.storeName,"abcd");
            StoreIndy.createDID();
            ItemsCollection = new ConcurrentHashMap<>();
            //there must be communication here to ask Steward for IndyLibraries.Endorser ROLE!
            String endpointSteward = StoreIndy.getEndPointFromLedger(stewardDID);
            String[] componentsOfEndpoint= endpointSteward.split(":");
            String endpoitTransportKey = StoreIndy.getDIDVerKeyFromLedger(stewardDID);//it is the DID verkey
            SocketChannel connectionTOSteward =SocketChannel.open();
            connectionTOSteward.connect(new InetSocketAddress(componentsOfEndpoint[0],
                    Integer.parseInt(componentsOfEndpoint[1])));
            String stewardDID2DID = StoreIndy.createConnection(connectionTOSteward.socket(),this.storeName);


            byte[]request=StoreIndy.writeMessage(StoreIndy.
                    askForEndorserRoleRequest(true),stewardDID2DID);
            StoreIndy.sendD2DMessage(request,connectionTOSteward.
                    socket());
            String response=new String((StoreIndy.waitForMessage(connectionTOSteward.
                    socket())),
                    Charset.defaultCharset());
            System.out.println("risposta steward creazione nym ruolo endorser "+ response);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
    public Store(String poolName,String storeName,String didValue,String walletName,String walletPassword){
        //creating the store, making public in the Ledger his DID and starting initializing his
        //endpoint for starting DID2DID comm

        try {
            Pool pool;
            Pool.setProtocolVersion(2).get();
            pool = Pool.openPoolLedger(poolName, "{}").get();
            this.pool = pool;
            this.storeName=storeName;
            sendingItemTo  = new ConcurrentHashMap<>();

            this.shippingid = 1;


            JSONUserCredentialStorage jsonStoredCred=null;
            File agentsFile=new File("./"+"agentsFile"+".json");
            ItemsCollection = new ConcurrentHashMap<>();
            jsonStoredCred= new JSONUserCredentialStorage(agentsFile);
            StoreIndy = new Endorser(pool,storeName,jsonStoredCred);
            //StoreIndy.CreateWallet("storeWallet1"+this.storeName,"abcd");
            StoreIndy.OpenWallet("storeWallet1"+this.storeName,"abcd");
            StoreIndy.getStoredDIDandVerkey(didValue);
            boolean inserted;
            System.out.println("insert Box Address HostIP to send Item");
            Scanner scanner= new Scanner(System.in);
            String ip=scanner.next();
            int port=scanner.nextInt();
            Store2Box useBOX=new Store2Box(this.StoreIndy,ip,port);
            boxes.add(useBOX);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
    public void inizializecred(SchemaStructure packageSchema,
                               CredDefStructure packageCredential, CredOfferStructure offerToClient, CredRequestStructure crs1){
        this.packageSchema=packageSchema;
        this.packageCredential=packageCredential;
        this.offerToClient=offerToClient;
        this.preFetchedREQ=crs1;
    }
    public void addItem(String itemName){
        this.ItemsCollection.put(itemName,new Item(itemName));
    }
    public SchemaStructure createPackageCredentialSchemaAndPublish(String schemaName,
                                                                   String schemaVersion,
                                                                   String[]schemaAttribs
                                                        ){
       /* this.StoreIndy.publishschema("SchemaPacco",
                "1.0",new String[]{"colli","stato","itemContenuto",
    "DIDDestinatario",
                        "DIDShippingAgent"});*/

        this.packageSchema= this.StoreIndy.publishschema(schemaName,
                schemaVersion,schemaAttribs);
        return this.packageSchema;
    }

    public CredDefStructure createPackageCredentialDefinitionAndPublish(String credDefTag, boolean supportRevocation,
                                                                        String schemaID){
        this.packageCredential=
         this.StoreIndy.IssuerCreateStoreAndPublishCredDef(credDefTag,true,schemaID);
        return this.packageCredential;
    }
    public void openStoreToClients(){
        try {
            AgentListenChannel =ServerSocketChannel.open();
            AgentListenChannel.socket().bind(null);
            listeningAddress = (InetSocketAddress) AgentListenChannel.getLocalAddress();

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("IndyStore Listening Address:" + listeningAddress);
        //add entpoint to NYM Object in Ledger to talk with Endorser
        //inserting address 127.0.0.1 for localhost

        StoreIndy.addENdpointToNYM("did2did","127.0.0.1:"+listeningAddress.getPort());
        System.out.println("Store aperto, ricerca mediante DID: "+ StoreIndy.mainDID.didName);

        Thread th = new Thread(new StoreRequestsHandler(this.pool,AgentListenChannel,this));
        th.start();

    }
    public String acceptConnection(Socket connectionSocket, String receivedMSG){
        //return the message of connection accepted with own DID2DID  comunication information
        String acceptedconn=StoreIndy.acceptConnection(receivedMSG,this.storeName);
        return acceptedconn;
    }

    public class marketPlaceStoreTaskHandler implements Runnable{
        private byte[] receivedRequest;
        private Store marketPlaceStore;
        private SocketChannel agentChannelToWrite;
        private boolean clientReady;
        private ReentrantLock lockState;//lock access shared variables with RequestHandler thread
        private Condition waitForTask;//condition variable to use while waiting to write answer
        private String clientDID;
        public marketPlaceStoreTaskHandler(byte[] receivedRequest, String clientDID, Store marketPlaceStore) {
            lockState=new ReentrantLock();
            waitForTask=lockState.newCondition();
            clientReady=false;
            this.receivedRequest=receivedRequest;
            this.marketPlaceStore = marketPlaceStore;
            this.clientDID=clientDID;
        }



        public void canGiveAnswer(SocketChannel agentChannelToWrite){
            lockState.lock();
            this.agentChannelToWrite=agentChannelToWrite;
            clientReady=true;//passata la socket channel del client a cui dare risposta
            waitForTask.signal();
            lockState.unlock();
        }

        @Override
        public void run() {
            System.out.println("task attiva!");
            byte[] request;
            String response;
            SocketChannel AgentChannel;
            ByteBuffer responseBuf;
            request = this.receivedRequest;
            response = marketPlaceStore.HandleRequest(request,this.clientDID)+"\n";
            System.out.println("response to SEND!"+ response + "clientreadystate:"+clientReady);
            lockState.lock();
            while (!clientReady) {
                try {
                    waitForTask.await();//attendo di ricevere la socketChannel del client
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            AgentChannel = this.agentChannelToWrite;
            lockState.unlock();
            try {
                System.out.println("writing");
                AgentChannel.configureBlocking(false);
                responseBuf = ByteBuffer.wrap(response.getBytes());
                while (responseBuf.hasRemaining()) {
                    try {
                        AgentChannel.write(responseBuf);
                    } catch (IOException e) {
                        e.printStackTrace();
                        AgentChannel.close();//close the channel if there is an interruption during the write
                    }
                }
                if (!responseBuf.hasRemaining()) {
                    responseBuf.clear();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("post write");
        }

    }
    //
    private String HandleRequest(byte[] request,String requester) {
        System.out.println("in handle request");
        String regexNospace = "([a-zA-Z0-9.~!$%^&*_=+_%\\[\\]()\"{}\\\\:;,^?-]+)"; // match only symbols
        String unpacked_request = StoreIndy.readMessage(request);
        JSONObject requestJSON=new JSONObject(unpacked_request);
        String returnMessage= unpacked_request;
        System.out.println("handling request1"+ unpacked_request);
        if(requestJSON.get("request").equals("Cred_Request_0")){ // send avaible Items at store
            System.out.println("handling request2");

            CredOfferStructure credOffer= StoreIndy.returnCredentialOffer(packageCredential.credDefId);
            JSONObject offerJSONObj= new JSONObject();
            offerJSONObj.put("credDefId",credOffer.credDef.credDefId);
            offerJSONObj.put("credDef",credOffer.credDef.credDefJson);
            offerJSONObj.put("credOffer",credOffer.credOffer);
            offerJSONObj.put("requesterDID",requester);
            offerJSONObj.put("revReg",credOffer.revRegId);
            offerJSONObj.put("Items", getAvailableItems());
            returnMessage = offerJSONObj.toString(4);
        }
        if(requestJSON.get("request").equals("Order_Item")){ // reserve item for client if still avaible
            String item = new JSONObject(requestJSON.getString("itemSelection")).getString("name");
            System.out.println(item);
            if(checkItemAvailabilityAndReserve(item)){
                returnMessage="Successfull Item Order";
                sendingItemTo.put(requester,item);
            }
            else{
                returnMessage="Item Not Available";
            }
        }
        if(requestJSON.get("request").equals("Cred_Request_1")){
            String toSend=sendingItemTo.remove(requester);
            String credReq= requestJSON.getString("credReq").toString();
            String credOffer = requestJSON.getString("credOffer").toString();
            System.out.println(credReq +"\n " +credOffer);

            if(toSend==null){
                returnMessage= "No Item Ordered";
            }
            else {
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
                Store2Box useBOX=null;
                boolean itemInserted= false;String ip;
                Integer port;
                Scanner scanner = new Scanner(System.in);
                int currentshippingid= 0;
                synchronized (this.shippingid){
                    currentshippingid=this.shippingid;
                    this.shippingid++;
                }
                String nonceForShipping= this.StoreIndy.generateNonce(); //Nonce is an unique identifier
                //(indy uses it for proof requests, it is an 80 bit number), indy support predicates of
                //predicate value of type int (32-bit)
                //use a truncated version of nonce
                nonceForShipping=String.valueOf(new BigInteger(nonceForShipping).intValue());

                if (boxes.isEmpty()){
                    System.out.println("insert Box Address HostIP to send Item");
                    ip=scanner.next();
                    port=scanner.nextInt();
                    useBOX=new Store2Box(this.StoreIndy,ip,port);

                    boxes.add(useBOX);
                    itemInserted=useBOX.sentItemToBOX(String.valueOf(currentshippingid),nonceForShipping,
                            packageCredential.credDefId);
                    while (!itemInserted){
                        System.out.println("Box was full, insert others Box AddressIP or Exit");
                        ip=scanner.next();
                        if (ip.equals("Exit")){
                            return "Failure all known boxes are full";
                        }
                        System.out.println("insert PortNum");
                        port=scanner.nextInt();
                        useBOX=new Store2Box(this.StoreIndy,ip,port);
                        boxes.add(useBOX);
                        itemInserted=useBOX.sentItemToBOX(String.valueOf(currentshippingid),nonceForShipping,
                                packageCredential.credDefId);

                    }

                }

                else{
                    int i=0;
                    while (!itemInserted  && i<boxes.size()){
                        useBOX=boxes.get(i);
                        itemInserted=useBOX.sentItemToBOX(String.valueOf(currentshippingid),nonceForShipping,
                                packageCredential.credDefId);

                        i++;
                    }
                    if(!itemInserted){
                        while (!itemInserted){
                            System.out.println("Box was full, insert others Box AddressIP or Exit");
                            ip=scanner.next();
                            if (ip.equals("Exit")){
                                return "Failure all known boxes are full";
                            }
                            System.out.println("insert PortNum");
                            port=scanner.nextInt();
                            useBOX=new Store2Box(this.StoreIndy,ip,port);
                            boxes.add(useBOX);
                            itemInserted=useBOX.sentItemToBOX(String.valueOf(currentshippingid),nonceForShipping,
                                    packageCredential.credDefId);

                        }
                    }
                }
                //versione ultra basic ------- solo accesso alla cassetta / accesso infinito
                CreateCredentialResult ret = this.StoreIndy.returnIssuerCreateCredentialNonRevocable(
                        this.packageSchema.schemaAttrs,
                        new String[]{String.valueOf(currentshippingid)
                                ,requester
                                ,useBOX.boxID
                                ,
                                toSend
                                ,this.StoreIndy.mainDID.didName
                                ,nonceForShipping},credOffer,
                        credReq);

                 return new JSONObject().put("credential",ret.credentialJson).put("BoxAddress",useBOX.boxAddress.toString()).toString();

            }
        }

        System.out.println("handling request3");
        return returnMessage;
    }
    public JSONArray getAvailableItems(){
        JSONObject itemobj;
        JSONArray itemsArray= new JSONArray();
        synchronized (ItemsCollection){
            for (Item i :
                 ItemsCollection.values()) {
                itemobj=new JSONObject();
                itemobj.put("name",i.name);
                if(i.DID!=null)
                    itemobj.put("did",i.DID);
                itemsArray.put(itemobj.toString());
            }
        }
        return itemsArray;
    }
    public boolean checkItemAvailabilityAndReserve(String obj){
        boolean ris;
        int index;
        synchronized (ItemsCollection) {
            if(ItemsCollection.remove(obj)!=null){
                ris=true;
            }
            else ris=false;
        }
        return ris;
    }




    //"SERVER THREAD"
    public static class StoreRequestsHandler implements Runnable {
        ServerSocketChannel AgentListenChannel;//serverSocketChannel per il multiplexing dei canali
        Pool pool;
        int foreignAgentID;
        Store marketPlaceStore;
        private ArrayList<String> foreignAGENTDID;

        public StoreRequestsHandler(Pool pool, ServerSocketChannel agentListenChannel, Store store) {
            this.pool = pool;
            this.AgentListenChannel = agentListenChannel;
            foreignAgentID=0;
            this.marketPlaceStore = store;
        }

        @Override
        public void run() {
            byte[] tmpTOString;
            Selector selector = null;
            Set<SelectionKey> readKeys;
            Iterator<SelectionKey> keyIterator;
            ByteBuffer byteBuffer;
            ArrayList<Byte> growingBuffer = new ArrayList<Byte>();
            HashMap<Integer, marketPlaceStoreTaskHandler> clientsWorker=new HashMap<>();//Store in HashMap AgentID->AgentHandler
            HashMap<Integer,String> foreignAGENTDID=new HashMap<>();// AgentId->AgentDID

            ThreadPoolExecutor taskToAgentExecutor=new ThreadPoolExecutor(2,5,60,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>());//corethread=2, maxPoolSize=5,

            String regexNospace="([a-zA-Z0-9.~!$%^&*_=+_%\\[\\]()\"{}\\\\:;,^?-]+)"; // match only symbols

            String receivedString;
            int i ;
            try {
                AgentListenChannel.configureBlocking(false);//selector needs Non blocking mode
                selector = Selector.open();
                AgentListenChannel.register(selector, SelectionKey.OP_ACCEPT);//wait for  TCP connection
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (true) {
                try {
                    selector.select();//search ready operation in the interest set
                } catch (IOException e) {
                    e.printStackTrace();
                }
                readKeys = selector.selectedKeys();
                keyIterator = readKeys.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();//prendo chiave
                    keyIterator.remove();//rimuovo chiave dal keyIterator

                    try {
                        if (key.isAcceptable()) {//Stabilimento Connessione TCP
                            SocketChannel client = ((ServerSocketChannel) key.channel()).accept();
                            client.configureBlocking(false);
                            SelectionKey k2 = client.register(selector, SelectionKey.OP_READ);//memorize  operation READ i interestSet
                            k2.attach(foreignAgentID++);
                            System.out.println("Connected to IndyLibraries.Agent: " + (foreignAgentID - 1));
                        } else if (key.isReadable()) {
                            marketPlaceStoreTaskHandler handler;
                            int id = (int) key.attachment();

                            byteBuffer = ByteBuffer.allocate(1024);//
                            SocketChannel ForeignAgent = (SocketChannel) key.channel();
                            ForeignAgent.configureBlocking(false);
                            byteBuffer.clear();
                            while (ForeignAgent.read(byteBuffer) > 0) {//Lettura della richiesta dal Client
                                byteBuffer.flip();
                                while (byteBuffer.hasRemaining()) {
                                    growingBuffer.add(byteBuffer.get());//lettura se byteBuffer.dimension()< (dati in arrivo)
                                }
                                byteBuffer.clear();
                            }
                            byteBuffer.flip();
                            while (byteBuffer.hasRemaining()) {
                                growingBuffer.add(byteBuffer.get());//ultima lettura
                            }
                            byteBuffer.clear();//il byteBuffer viene ripulito per operazioni successive
                            if (growingBuffer.size() == 0) {//TCP connection was closed in the other side
                                System.out.print("TCP connection with Client Fail: " + foreignAgentID);
                                ForeignAgent.close();
                            } else {
                                key.interestOps(SelectionKey.OP_WRITE);//È arrivando un comando,vuol dire che ForeignAgent aspetterà una risposta,
                                //mettiamo nell'interestSet la scrittura nel canale del ForeignAgent

                                tmpTOString = new byte[growingBuffer.size()];//bytes
                                for (i = 0; i < growingBuffer.size(); i++) {
                                    tmpTOString[i] = growingBuffer.get(i);
                                }
                                growingBuffer.clear();//pulizia dell'arraylist per operazioni future
                                receivedString = new String(tmpTOString, Charset.defaultCharset());//trasmormo i byte della richiesta in una stringa con la richiesta stessa
                                if (foreignAGENTDID.get(id) == null) {
                                    foreignAGENTDID.put(id,marketPlaceStore.acceptConnection(ForeignAgent.socket(),receivedString));
                                } else if (receivedString.equals("DISCONNECT")) {//Logout totalmente effettuato nel MainThread-Server
                                    foreignAGENTDID.remove(id);//remove the DID of the Foreign from the Know DID list
                                    key.cancel();
                                    key.channel().close();//closed channel with ForeignAgent


                                } else {

                                    System.out.println("created an handler for a request of IndyLibraries.Agent: " + foreignAGENTDID.get(id));
                                    //we need the byte
                                    handler =
                                            this.marketPlaceStore.new marketPlaceStoreTaskHandler(tmpTOString,
                                                    foreignAGENTDID.get(id),
                                                    this.marketPlaceStore);
                                    clientsWorker.put(id, handler);
                                    taskToAgentExecutor.submit(handler);

                                }
                            }
                        } else if (key.isWritable()) {//Quando il Client è pronto a ricevere risposta
                            int id = (int) key.attachment();
                            marketPlaceStoreTaskHandler handler;
                            if (clientsWorker.containsKey(id)) {//SE non è presenta allora vuol dire che non
                                //si è letta richiesta in precedenza
                                System.out.println("can give answer");
                                SocketChannel client = (SocketChannel) key.channel();
                                handler = clientsWorker.remove(id);//get and remove handler reference
                                handler.canGiveAnswer(client);//give channel to write answer
                                key.interestOps(SelectionKey.OP_READ);//set  OP_READ in interestSet
                                //wait new request from this client
                            }
                            else {//if there is no handler assigned to this agent then it is in did2did communication setup phase

                                JSONObject DID2DIDSetupData=new JSONObject(foreignAGENTDID.get(id));
                                //splitting in a convenientway the did and the rest
                                System.out.println(DID2DIDSetupData.get("theirDID"));
                                key.channel().configureBlocking(false);
                                ByteBuffer toSendBuf = ByteBuffer.wrap(DID2DIDSetupData.getString("message").getBytes(StandardCharsets.UTF_8));
                                while (toSendBuf.hasRemaining()) {
                                    try {
                                        ((SocketChannel)key.channel()).write(toSendBuf);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        key.channel().close();//close the channel if there is an interruption during the write
                                    }
                                }
                                if (!toSendBuf.hasRemaining()) {
                                    toSendBuf.clear();
                                }
                                foreignAGENTDID.put(id,DID2DIDSetupData.getString("theirDID"));
                                key.interestOps(SelectionKey.OP_READ);//  OP_READ in interestSet

                            }
                        }
                    } catch (ClosedChannelException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    private class Store2Box{
        public String boxID;
        Endorser StoreIndy;
        String box2StoreDID;
        InetSocketAddress boxAddress;
        DatagramSocket ds;

        public Store2Box(       Endorser StoreIndy,String boxHostname,int boxHostPort
        ){
            this.boxAddress=boxAddress;
            this.StoreIndy = StoreIndy;
            boxAddress=new InetSocketAddress(boxHostname,boxHostPort);
            ds=null;
            byte[]msg;
            String message;
            try {
                ds = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            }
            askForConnectionToBox();
        }

        public void askForConnectionToBox(){
            box2StoreDID = StoreIndy.createDID2DIDComunication(storeName, boxAddress,this.ds);
            JSONObject messageJOBJECT= new JSONObject();
            messageJOBJECT.put("request","GetBoxID");
            byte [] msg=StoreIndy.writeMessage(messageJOBJECT.toString(),box2StoreDID);
            sendMSG(msg,this.boxAddress);
            //box sends his ID to Store
            this.boxID =receiveMSG(ds);
        }
        public boolean sentItemToBOX(String shippingid,String shippingnonce,String creddefid){
            JSONObject messageJOBJECT= new JSONObject();
            messageJOBJECT.put("shippingid",shippingid);
            messageJOBJECT.put("creddefid",creddefid);
            messageJOBJECT.put("shippingnonce",shippingnonce);
            messageJOBJECT.put("request","InsertItem");
            byte [] msg=StoreIndy.writeMessage(messageJOBJECT.toString(),box2StoreDID);
            sendMSG(msg,this.boxAddress);
            return receiveMSG(ds).equals("Success");
        }
        private void sendMSG(String msg,InetSocketAddress theirAddress) {
            byte[] toSend = msg.getBytes();
            try  {
                DatagramPacket datagramPacket = new DatagramPacket(toSend, toSend.length,
                        theirAddress.getAddress(),theirAddress.getPort());
                this.ds.send(datagramPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private void sendMSG(byte[] toSend,InetSocketAddress theirAddress) {
            try  {
                DatagramPacket datagramPacket = new DatagramPacket(toSend, toSend.length,
                        theirAddress.getAddress(),theirAddress.getPort());
                this.ds.send(datagramPacket);
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

            message = StoreIndy.readMessage(dataArray);

            return message;


        }

    }

}
