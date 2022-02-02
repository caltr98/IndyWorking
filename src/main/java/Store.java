import IndyLibraries.*;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.pool.Pool;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.NoSuchPaddingException;
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
    private String storeName;
    private ServerSocketChannel AgentListenChannel;
    private InetSocketAddress listeningAddress;
    private ConcurrentHashMap<String,Item> ItemsCollection;//ItemName->Item


    protected Endorser StoreIndy;
    protected Integer shippingid;
    private Pool pool;
    protected CredDefStructure shippingCredential;
    protected SchemaStructure shippingSchema;
    protected ConcurrentHashMap<String,String> sendingItemTo;
    protected ArrayList <Store2Box> boxes ;
    protected ArrayList <Store2ShippingAgent> shippingAgents;
    protected CredDefStructure shipmentCredential;
    private SchemaStructure shipmentSchema;
    protected Integer shipmentid;

    public Store(Pool handle, String StoreDIDseed, String StoreName,String storeWallet,String walletPassword){
        //creating the store, using a pool provided by the creator, allows to connect to an already
        // allows to connect to a pool different than Indyscan pool.
        //Predefined DID seed, allows to connect to Sovrin Buildernet with an Endorser DID
        // (https://selfserve.sovrin.org/)
        boxes = new ArrayList<>();
        shippingAgents = new ArrayList<>();
        try {
            this.pool = handle;
            this.storeName=StoreName;
            this.shippingid = 1;
            this.shipmentid= new Random().nextInt(1000);
            JSONUserCredentialStorage jsonStoredCred=null;
            File agentsFile=new File("./"+"agentsFile"+".json");
            sendingItemTo  = new ConcurrentHashMap<>();
            jsonStoredCred= new JSONUserCredentialStorage(agentsFile);
            StoreIndy = new Endorser(pool,storeName,jsonStoredCred);
            StoreIndy.CreateWallet("storeWallet"+this.storeName,walletPassword);
            StoreIndy.OpenWallet("storeWallet"+this.storeName,walletPassword);
            StoreIndy.createDID(StoreDIDseed);
            ItemsCollection = new ConcurrentHashMap<>();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public Store(String poolName,String storeName,String walletPassword){
        //creating the store, making public in the Ledger his DID and starting initializing his
        //endpoint for starting DID2DID comm
        boxes = new ArrayList<>();
        shippingAgents = new ArrayList<>();
        System.out.println("Insert a known Steward DID");
        Scanner sc = new Scanner(System.in);
        String stewardDID=sc.next();
        try {
            Pool pool;
            Pool.setProtocolVersion(2).get();
            pool = Pool.openPoolLedger(poolName, "{}").get();
            this.pool = pool;
            this.storeName=storeName;
            this.shippingid = 1;
            this.shipmentid= new Random().nextInt(1000);
            JSONUserCredentialStorage jsonStoredCred=null;
            File agentsFile=new File("./"+"agentsFile"+".json");
            sendingItemTo  = new ConcurrentHashMap<>();
            jsonStoredCred= new JSONUserCredentialStorage(agentsFile);
            StoreIndy = new Endorser(pool,storeName,jsonStoredCred);
            StoreIndy.CreateWallet("storeWallet"+this.storeName,walletPassword);
            StoreIndy.OpenWallet("storeWallet"+this.storeName,walletPassword);
            StoreIndy.createDID();
            ItemsCollection = new ConcurrentHashMap<>();
            //Store must ask Steward for Endorser role
            String endpointSteward = StoreIndy.getEndPointFromLedger(stewardDID);
            String[] componentsOfEndpoint= endpointSteward.split(":");
            SocketChannel connectionTOSteward =SocketChannel.open();
            connectionTOSteward.connect(new InetSocketAddress(componentsOfEndpoint[0],
                    Integer.parseInt(componentsOfEndpoint[1])));
            //DID to DID communication setup
            String store_to_stewardDID2DID = StoreIndy.createConnection(connectionTOSteward.socket(),this.storeName);
            //Message ask for Endorser role creation
            byte[]request=StoreIndy.writeMessage(StoreIndy.
                    askForEndorserRoleRequest(true),store_to_stewardDID2DID);
            StoreIndy.sendD2DMessage(request,connectionTOSteward.
                    socket());
            //read response
            String response=new String((StoreIndy.waitForMessage(connectionTOSteward.
                    socket())),
                    Charset.defaultCharset());
        } catch (IOException | InterruptedException | NoSuchAlgorithmException | ExecutionException | NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (IndyException e) {
            e.printStackTrace();
        }
    }

    public Endorser getStoreIndy() {
        return StoreIndy;
    }

    public Store(String poolName,String storeName,String didValue,String walletPassword){
        //creating the store, making public in the Ledger his DID and starting initializing his
        //endpoint for starting DID2DID comm
        boxes = new ArrayList<>();
        shippingAgents = new ArrayList<>();

        try {
            Pool pool;
            Pool.setProtocolVersion(2).get();
            pool = Pool.openPoolLedger(poolName, "{}").get();
            this.pool = pool;
            this.storeName=storeName;
            sendingItemTo  = new ConcurrentHashMap<>();
            this.shippingid = 1;
            this.shipmentid= new Random().nextInt(1000);
            JSONUserCredentialStorage jsonStoredCred=null;
            File agentsFile=new File("./"+"agentsFile"+".json");
            ItemsCollection = new ConcurrentHashMap<>();
            jsonStoredCred= new JSONUserCredentialStorage(agentsFile);
            StoreIndy = new Endorser(pool,storeName,jsonStoredCred);
            StoreIndy.OpenWallet("storeWallet"+this.storeName,walletPassword);
            StoreIndy.getStoredDIDandVerkey(didValue);
            System.out.println("insert Box Address HostIP to send Item,HostIP is 127.0.0.1, INSERT PortNo");
            Scanner scanner= new Scanner(System.in);
            String ip="127.0.0.1";
            int port=scanner.nextInt();
            Store2Box useBOX=new Store2Box(this.StoreIndy,"127.0.0.1",port);
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
    public void addItem(String itemName){
        this.ItemsCollection.put(itemName,new Item(itemName));
    }

    public SchemaStructure createPackageCredentialSchemaAndPublish(String schemaName,
                                                                   String schemaVersion,
                                                                   String[]schemaAttribs
                                                        ){
        this.shippingSchema = this.StoreIndy.publishschema(schemaName,
                schemaVersion,schemaAttribs);
        return this.shippingSchema;
    }

    public CredDefStructure createPackageCredentialDefinitionAndPublish(String credDefTag, boolean supportRevocation,
                                                                        String schemaID){
        this.shippingCredential =
         this.StoreIndy.IssuerCreateStoreAndPublishCredDef(credDefTag,supportRevocation,schemaID);
        return this.shippingCredential;
    }
    public SchemaStructure createShippingCredentialSchemaAndPublish(String schemaName,
                                                                   String schemaVersion,
                                                                   String[]schemaAttribs
    ){

        this.shipmentSchema = this.StoreIndy.publishschema(schemaName,
                schemaVersion,schemaAttribs);
        return this.shipmentSchema;
    }

    public CredDefStructure createShippingCredentialDefinitionAndPublish(String credDefTag, boolean supportRevocation,
                                                                        String schemaID){
        this.shipmentCredential =
                this.StoreIndy.IssuerCreateStoreAndPublishCredDef(credDefTag,supportRevocation,schemaID);
        return this.shipmentCredential;
    }

    public SchemaStructure createPackageCredentialSchemaAndPublishOnSovrin(String schemaName,
                                                                   String schemaVersion,
                                                                   String[]schemaAttribs,String taaDigest,
                                                                           String taaAcceptanceMechanism,
                                                                           Long acceptanceTimestamp
    ){
        this.shippingSchema = this.StoreIndy.publishschemaOnSovrin(schemaName,
                schemaVersion,schemaAttribs,taaDigest,taaAcceptanceMechanism,acceptanceTimestamp);
        return this.shippingSchema;
    }

    public CredDefStructure createPackageCredentialDefinitionAndPublishOnSovrin(String credDefTag, boolean supportRevocation,
                                                                        String schemaID, String taaDigest,
                                                                                String taaAcceptanceMechanism,
                                                                                Long acceptanceTimestamp){
        this.shippingCredential =
                this.StoreIndy.IssuerCreateStoreAndPublishCredDefOnSovrin(credDefTag,supportRevocation,schemaID,taaDigest,
                        taaAcceptanceMechanism,acceptanceTimestamp);
        return this.shippingCredential;
    }
    public SchemaStructure createShippingCredentialSchemaAndPublishOnSovrin(String schemaName,
                                                                    String schemaVersion,
                                                                    String[]schemaAttribs,String taaDigest,
                                                                            String taaAcceptanceMechanism,
                                                                            Long acceptanceTimestamp)
    {

        this.shipmentSchema = this.StoreIndy.publishschemaOnSovrin(schemaName,
                schemaVersion,schemaAttribs,taaDigest,taaAcceptanceMechanism,acceptanceTimestamp);
        return this.shipmentSchema;
    }

    public CredDefStructure createShippingCredentialDefinitionAndPublishOnSovrin(String credDefTag, boolean supportRevocation,
                                                                                 String schemaID, String taaDigest,
                                                                                 String taaAcceptanceMechanism,
                                                                                 Long acceptanceTimestamp){
        this.shipmentCredential =
                this.StoreIndy.IssuerCreateStoreAndPublishCredDefOnSovrin(credDefTag,supportRevocation,schemaID,taaDigest,
                        taaAcceptanceMechanism,acceptanceTimestamp);
        return this.shipmentCredential;
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
        System.out.println("Store is open, search with DID: "+ StoreIndy.mainDID.didName);

        Thread th = new Thread(new StoreRequestsHandler(this.pool,AgentListenChannel,this));
        //starting StoreRequestHandler in the Store MainThread.
        th.start();

    }
    public String acceptConnection(Socket connectionSocket, String receivedMSG){
        //return the message of connection accepted with own DID2DID  communication information
        String acceptedconn=StoreIndy.acceptConnection(receivedMSG,this.storeName);
        return acceptedconn;
    }

    public void openStoreToClientsOnSovrin(String taaDigest,
                                           String taaAcceptanceMechanism,
                                           Long acceptanceTimestamp) {
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

        StoreIndy.addENdpointToNYMOnSovrin("did2did","127.0.0.1:"+listeningAddress.getPort(),taaDigest,
                taaAcceptanceMechanism,acceptanceTimestamp);
        System.out.println("Store aperto, ricerca mediante DID: "+ StoreIndy.mainDID.didName);
        System.out.println("Store is open, search with DID: "+ StoreIndy.mainDID.didName);

        Thread th = new Thread(new StoreRequestsHandler(this.pool,AgentListenChannel,this));
        //starting StoreRequestHandler in the Store MainThread.
        th.start();

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


        //StoreRequestHandler-Thread returns the SocketChannel to write an answer to the client
        public void canGiveAnswer(SocketChannel agentChannelToWrite){
            lockState.lock();
            this.agentChannelToWrite=agentChannelToWrite;
            clientReady=true;//passata la socket channel del client a cui dare risposta
            waitForTask.signal();
            lockState.unlock();
        }

        @Override
        public void run() {
            byte[] request;
            String response;
            SocketChannel AgentChannel;
            ByteBuffer responseBuf;
            request = this.receivedRequest;
            response = marketPlaceStore.HandleRequest(request,this.clientDID)+"\n";
            //Thread task handler waits for the SocketChannel to be writable, the StoreRequestHandler-Thread will
            //signal with with the method canGiveAnswer
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
            //sending response to client
            try {
                AgentChannel.configureBlocking(false);
                responseBuf = ByteBuffer.wrap(response.getBytes());
                while (responseBuf.hasRemaining()) {
                    try {
                        AgentChannel.write(responseBuf);
                    } catch (IOException e) {
                        e.printStackTrace();
                        AgentChannel.close();//close the channel if there is an interruption while writing
                    }
                }
                if (!responseBuf.hasRemaining()) {
                    responseBuf.clear();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    //
    protected String HandleRequest(byte[] request,String requester) {
        System.out.println("in handle request");
        String regexNospace = "([a-zA-Z0-9.~!$%^&*_=+_%\\[\\]()\"{}\\\\:;,^?-]+)"; // match only symbols
        String unpacked_request = StoreIndy.readMessage(request);
        JSONObject requestJSON=new JSONObject(unpacked_request);
        String returnMessage= unpacked_request;
        int i;
        System.out.println("handling request1"+ unpacked_request);
        if(requestJSON.get("request").equals("Cred_Request_0")){ // send list of available Items at store
            //and credential offer
            System.out.println("handling request2");

            CredOfferStructure credOffer= StoreIndy.returnCredentialOffer(shippingCredential.credDefId);
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
                boolean bookedBox= false;String ip;
                Integer port;
                Scanner scanner = new Scanner(System.in);
                int currentshippingid= 0;
                int currentshipmentid = 0;
                synchronized (this.shippingid){
                    currentshippingid=this.shippingid;
                    this.shippingid++;
                }
                synchronized (this.shipmentid){
                    currentshipmentid=this.shipmentid;
                    this.shipmentid++;
                }
                String nonceForShipping= this.StoreIndy.generateNonce(); //Nonce is an unique identifier
                //(indy uses it for proof requests, it is an 80 bit number), indy support predicates of
                //predicate value of type int (32-bit)
                //use a truncated version of nonce
                String nonceForShipment=this.StoreIndy.generateNonce();
                nonceForShipping=String.valueOf(new BigInteger(nonceForShipping).intValue());
                nonceForShipment=String.valueOf(new BigInteger(nonceForShipment).intValue());

                if (boxes.isEmpty()){
                    System.out.println("insert Box Address HostIP to send Item,HostIP is 127.0.0.1, INSERT PortNo");
                    //ip=scanner.next();
                    ip="127.0.0.1";
                    port=scanner.nextInt();
                    useBOX=new Store2Box(this.StoreIndy,ip,port);

                    boxes.add(useBOX);
                    bookedBox=useBOX.reserveBoxForItem(String.valueOf(currentshippingid),nonceForShipping,
                            String.valueOf(currentshipmentid),
                            nonceForShipment,
                            shippingCredential.credDefId, shipmentCredential.credDefId);
                    while (!bookedBox){
                        System.out.println("Box was full, insert others Box AddressIP or Exit");
                        ip=scanner.next();
                        if (ip.equals("Exit")){
                            return "Failure all known boxes are full";
                        }
                        System.out.println("insert PortNum");
                        port=scanner.nextInt();
                        useBOX=new Store2Box(this.StoreIndy,ip,port);
                        boxes.add(useBOX);
                        bookedBox=useBOX.reserveBoxForItem(String.valueOf(currentshippingid),nonceForShipping,
                                String.valueOf(currentshipmentid),
                                nonceForShipment,
                                shippingCredential.credDefId, shipmentCredential.credDefId);

                    }

                }

                else{
                    i=0;
                    while (!bookedBox  && i<boxes.size()){
                        useBOX=boxes.get(i);
                        bookedBox=useBOX.reserveBoxForItem(String.valueOf(currentshippingid),nonceForShipping,
                                String.valueOf(currentshipmentid),
                                nonceForShipment,
                                shippingCredential.credDefId, shipmentCredential.credDefId);
                        i++;
                    }
                    if(!bookedBox){
                        while (!bookedBox){
                            System.out.println("Box was full,type 1 to abort operation");

                            ip= String.valueOf(scanner.nextInt());
                            if (ip.equals("1")){
                                return "Failure all known boxes are full";
                            }

                            System.out.println("insert Box Address HostIP to send Item,HostIP is 127.0.0.1, INSERT PortNo");
                            //ip=scanner.next();
                            ip="127.0.0.1";
                            port=scanner.nextInt();
                            useBOX=new Store2Box(this.StoreIndy,ip,port);
                            boxes.add(useBOX);
                            bookedBox=useBOX.reserveBoxForItem(String.valueOf(currentshippingid),nonceForShipping,
                                    String.valueOf(currentshipmentid),
                                    nonceForShipment,
                                    shippingCredential.credDefId, shipmentCredential.credDefId);

                        }
                    }
                }
                Store2ShippingAgent store2shippingAgent;
                //request shipping to ShipperAgent
                if(shippingAgents.isEmpty()){
                    System.out.println("insert ShippingDeliverer HostIP to send Item,HostIP is 127.0.0.1, INSERT PortNo");
                    //ip=scanner.next();
                    ip="127.0.0.1";
                    port=scanner.nextInt();
                    store2shippingAgent=new Store2ShippingAgent(this.StoreIndy,ip,port);
                }
                else{
                    System.out.println("Avaible Shipping Agents");
                    for (i = 0; i < shippingAgents.size() ; i++) {
                        System.out.println(i+")"+" - "+shippingAgents.get(i).shippingAgentID);
                    }
                    System.out.println("Select Shipping Agent Number or another number to add a new Shipping Agent");
                    i=scanner.nextInt();
                    if(i<shippingAgents.size() && i>=0){
                        store2shippingAgent=shippingAgents.get(i);
                    }
                    else{
                        System.out.println("insert ShippingDeliverer HostIP to send Item,HostIP is 127.0.0.1, INSERT PortNo");
                        //ip=scanner.next();
                        ip="127.0.0.1";
                        port=scanner.nextInt();
                        store2shippingAgent=new Store2ShippingAgent(this.StoreIndy,ip,port);
                    }
                }
                System.out.println("How many hours should the LockerBox contain the Item?");
                i=scanner.nextInt();
                while(i<=0 ){
                    System.out.println("Package should be in the LockerBox for at least 1h");
                    System.out.println("How many hours should the LockerBox contain the Item?");
                    i=scanner.nextInt();
                }
                store2shippingAgent.giveShipmentToAgent( new String[]{ String.valueOf(currentshipmentid),(int)((Math.
                                random()*79 )+1 )+"kg",(int)((Math.
                                random()*999 )+1 )+"euros"
                                ,nonceForShipment,
                                String.valueOf(((int) (Math.random()*71) +1))},
                        useBOX.boxAddress.getAddress().getHostAddress(),useBOX.boxAddress.getPort());

                CreateCredentialResult ret = this.StoreIndy.returnIssuerCreateCredentialNonRevocable(
                        this.shippingSchema.schemaAttrs,
                        new String[]{String.valueOf(currentshippingid)
                                ,requester
                                ,useBOX.boxID
                                ,
                                toSend
                                ,this.StoreIndy.mainDID.didName
                                ,nonceForShipping},credOffer,
                        credReq);
                System.out.println("Store to Customer credential size in bytes"+ ret.credentialJson.getBytes(StandardCharsets.UTF_8).length);


                System.out.println("Post Store  sending Item metrics:\n"+StoreIndy.collectMetrics());

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
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    try {
                        if (key.isAcceptable()) {// TCP connection setup
                            SocketChannel client = ((ServerSocketChannel) key.channel()).accept();
                            client.configureBlocking(false);
                            SelectionKey k2 = client.register(selector, SelectionKey.OP_READ);//registering the operation READ im interestSet
                            k2.attach(foreignAgentID++);
                            System.out.println("Connected to IndyLibraries.Agent: " + (foreignAgentID - 1));
                        } else if (key.isReadable()) {
                            marketPlaceStoreTaskHandler handler;
                            int id = (int) key.attachment();

                            byteBuffer = ByteBuffer.allocate(1024);//
                            SocketChannel ForeignAgent = (SocketChannel) key.channel();
                            ForeignAgent.configureBlocking(false);
                            byteBuffer.clear();
                            while (ForeignAgent.read(byteBuffer) > 0) {//reading client request
                                byteBuffer.flip();
                                while (byteBuffer.hasRemaining()) {
                                    growingBuffer.add(byteBuffer.get());//reading from  byteBuffer.dimension()< (incoming data)
                                }
                                byteBuffer.clear();
                            }
                            byteBuffer.flip();
                            while (byteBuffer.hasRemaining()) {
                                growingBuffer.add(byteBuffer.get());//last read
                            }
                            byteBuffer.clear();//il byteBuffer cleared for future usages
                            if (growingBuffer.size() == 0) {//TCP connection was closed on the other side
                                System.out.print("TCP connection with Client Fail: " + foreignAgentID);
                                ForeignAgent.close();
                            } else {
                                key.interestOps(SelectionKey.OP_WRITE);//after receiving a request the interest set of the selected key
                                //should be that of  write operation, to write the response
                                tmpTOString = new byte[growingBuffer.size()];//bytes
                                for (i = 0; i < growingBuffer.size(); i++) {
                                    tmpTOString[i] = growingBuffer.get(i);
                                }
                                growingBuffer.clear();//clearing array list for future operations
                                receivedString = new String(tmpTOString, Charset.defaultCharset());//trasmormo i byte della richiesta in una stringa con la richiesta stessa
                                if (foreignAGENTDID.get(id) == null) {
                                    foreignAGENTDID.put(id,marketPlaceStore.acceptConnection(ForeignAgent.socket(),receivedString));
                                } else if (receivedString.equals("DISCONNECT")) {//disconnecting from the store
                                    foreignAGENTDID.remove(id);//remove the DID of the Foreign from the Know DID list
                                    key.cancel();
                                    key.channel().close();//closed channel with ForeignAgent


                                } else {

                                    System.out.println("created an handler for a request of IndyLibraries.Agent: " + foreignAGENTDID.get(id));
                                    handler =
                                            this.marketPlaceStore.new marketPlaceStoreTaskHandler(tmpTOString,
                                                    foreignAGENTDID.get(id),
                                                    this.marketPlaceStore);

                                    clientsWorker.put(id, handler);
                                    taskToAgentExecutor.submit(handler);

                                }
                            }
                        } else if (key.isWritable()) {//Client is ready to write response
                            int id = (int) key.attachment();
                            marketPlaceStoreTaskHandler handler;
                            if (clientsWorker.containsKey(id)) {
                                System.out.println("can give answer");
                                SocketChannel client = (SocketChannel) key.channel();
                                handler = clientsWorker.remove(id);//get and remove handler reference
                                handler.canGiveAnswer(client);//give channel to write answer
                                key.interestOps(SelectionKey.OP_READ);//set  OP_READ in interestSet
                                //wait new request from this client
                            }
                            else {//if there is no handler assigned to this agent then it is in did2did communication setup phase

                                JSONObject DID2DIDSetupData=new JSONObject(foreignAGENTDID.get(id));
                                //splitting in a convenient the did and the remainder of the message
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

    protected class Store2Box{
        public String boxID;
        Endorser StoreIndy;
        String box2StoreDID;
        InetSocketAddress boxAddress;
        DatagramSocket ds;

        public Store2Box(       Endorser StoreIndy,String boxHostname,int boxHostPort
        ){
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
            box2StoreDID = StoreIndy.createDID2DIDCommunication(storeName, boxAddress,this.ds);
            JSONObject messageJOBJECT= new JSONObject();
            messageJOBJECT.put("request","GetBoxID");
            byte [] msg=StoreIndy.writeMessage(messageJOBJECT.toString(),box2StoreDID);
            sendMSG(msg,this.boxAddress);
            //box sends his ID to Store
            this.boxID =receiveMSG(ds);
        }
        public boolean reserveBoxForItem(String shippingid, String shippingnonce, String shipmentid, String nonceForShipment,
                                         String shippingCredDefId, String shipmentcredDefId){
            JSONObject messageJOBJECT= new JSONObject();
            messageJOBJECT.put("shippingid",shippingid);
            messageJOBJECT.put("shippingnonce",shippingnonce);
            messageJOBJECT.put("shippingcreddefid",shippingCredDefId);

            messageJOBJECT.put("shipmentid",shipmentid);
            messageJOBJECT.put("shipmentnonce",nonceForShipment);
            messageJOBJECT.put("shipmentcredefid",shipmentcredDefId);
            messageJOBJECT.put("storedid",this.StoreIndy.mainDID.didName);
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
    protected class Store2ShippingAgent{
        public String shippingAgentID;
        Endorser StoreIndy;
        String ShippingAgent2StoreDID;
        InetSocketAddress boxAddress;
        DatagramSocket ds;
        private long deliveryId;

        public Store2ShippingAgent(
                Endorser StoreIndy,String ShippingAgentHostname,int ShippingAgentHostPort
        ){
            this.StoreIndy = StoreIndy;
            boxAddress=new InetSocketAddress(ShippingAgentHostname,ShippingAgentHostPort);
            ds=null;
            byte[]msg;
            String message;
            try {
                ds = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            }
            askForConnectionToShippingAgent();

        }
        public void askForConnectionToShippingAgent(){
            this.ShippingAgent2StoreDID = StoreIndy.createDID2DIDCommunication(storeName, boxAddress,this.ds);
            JSONObject messageJOBJECT= new JSONObject();
            messageJOBJECT.put("request","GetShippingAgentID");
            byte [] msg=StoreIndy.writeMessage(messageJOBJECT.toString(),this.ShippingAgent2StoreDID);
            sendMSG(msg,this.boxAddress);
            //ShippingDeliverer sends his ID to Store
            this.shippingAgentID =receiveMSG(ds);
        }

        protected boolean giveShipmentToAgent(String[] attrValues, String boxAddress, int boxPort){
            JSONObject messageJOBJECT= new JSONObject();
            messageJOBJECT.put("request","HandleDelivery0");
            CredOfferStructure credOffer= StoreIndy.returnCredentialOffer(
                    shipmentCredential.credDefId);

            System.out.println("Store CredentialOffer to ShippingDeliverer size in bytes"+ credOffer.credOffer.getBytes(StandardCharsets.UTF_8).length+" bytes");
            JSONObject offerJSONObj= new JSONObject();
            offerJSONObj.put("credDefId",credOffer.credDef.credDefId);
            offerJSONObj.put("credDef",credOffer.credDef.credDefJson);
            offerJSONObj.put("credOffer",credOffer.credOffer);
            offerJSONObj.put("requesterDID",ShippingAgent2StoreDID);
            offerJSONObj.put("revReg",credOffer.revRegId);
            messageJOBJECT.put("itemCredentialOffer",offerJSONObj.toString(4));
            byte [] msg=StoreIndy.writeMessage(messageJOBJECT.toString(),ShippingAgent2StoreDID);
            sendMSG(msg,this.boxAddress);
            String identifer= receiveMSG(ds);
            JSONObject credReqOBJ;

            if(!identifer.equals("Error")){
                credReqOBJ= new JSONObject(identifer);
                this.deliveryId = credReqOBJ.getLong("deliveryId");
            }
            String credfRequest= receiveMSG(ds);
            if(!credfRequest.equals("Error")){
                credReqOBJ = new JSONObject(credfRequest);
                String credReq= credReqOBJ.getString("credReq");
                String fromRequesterOffer = credReqOBJ.getString("credOffer");
                CreateCredentialResult ret = this.StoreIndy.returnIssuerCreateCredentialNonRevocable(
                        shipmentSchema.schemaAttrs, attrValues,fromRequesterOffer,
                        credReq);
                 String tosend=new JSONObject().put("Credential",ret.credentialJson).put("request","HandleDelivery1").put("LockerBoxAgentIP",boxAddress).
                         put("LockerBoxAgentPortNo",boxPort).put("deliveryId",this.deliveryId).toString();
                 msg=StoreIndy.writeMessage(tosend,ShippingAgent2StoreDID);
                 sendMSG(msg,this.boxAddress);

                System.out.println("Store Credential created for ShippingDeliverer size in bytes"+ ret.credentialJson.getBytes(StandardCharsets.UTF_8).length+" bytes");
                System.out.println("Store CredentialMSG  ShippingDeliverer size in bytes"+ msg.length+" bytes");

                return true;
            }
            return false;
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
        protected boolean giveShipmentToAgent(String[] attrValues, String boxAddress, int boxPort,String deliveryMessage){
            JSONObject messageJOBJECT= new JSONObject();
            messageJOBJECT.put("request","HandleDelivery0");
            CredOfferStructure credOffer= StoreIndy.returnCredentialOffer(
                    shipmentCredential.credDefId);

            System.out.println("Store CredentialOffer to ShippingDeliverer size in bytes"+ credOffer.credOffer.getBytes(StandardCharsets.UTF_8).length+" bytes");
            JSONObject offerJSONObj= new JSONObject();
            offerJSONObj.put("credDefId",credOffer.credDef.credDefId);
            offerJSONObj.put("credDef",credOffer.credDef.credDefJson);
            offerJSONObj.put("credOffer",credOffer.credOffer);
            offerJSONObj.put("requesterDID",ShippingAgent2StoreDID);
            offerJSONObj.put("revReg",credOffer.revRegId);
            messageJOBJECT.put("itemCredentialOffer",offerJSONObj.toString(4));
            byte [] msg=StoreIndy.writeMessage(messageJOBJECT.toString(),ShippingAgent2StoreDID);
            sendMSG(msg,this.boxAddress);
            String identifer= receiveMSG(ds);
            JSONObject credReqOBJ;

            if(!identifer.equals("Error")){
                credReqOBJ= new JSONObject(identifer);
                this.deliveryId = credReqOBJ.getLong("deliveryId");
            }
            String credfRequest= receiveMSG(ds);
            if(!credfRequest.equals("Error")){
                credReqOBJ = new JSONObject(credfRequest);
                String credReq= credReqOBJ.getString("credReq");
                String fromRequesterOffer = credReqOBJ.getString("credOffer");
                CreateCredentialResult ret = this.StoreIndy.returnIssuerCreateCredentialNonRevocable(
                        shipmentSchema.schemaAttrs, attrValues,fromRequesterOffer,
                        credReq);
                String tosend=new JSONObject().put("Credential",ret.credentialJson).put("request","HandleDelivery1").put("LockerBoxAgentIP",boxAddress).
                        put("LockerBoxAgentPortNo",boxPort).put("deliveryId",this.deliveryId).put("message",deliveryMessage).toString();
                msg=StoreIndy.writeMessage(tosend,ShippingAgent2StoreDID);
                sendMSG(msg,this.boxAddress);

                System.out.println("Store Credential created for ShippingDeliverer size in bytes"+ ret.credentialJson.getBytes(StandardCharsets.UTF_8).length+" bytes");
                System.out.println("Store CredentialMSG  ShippingDeliverer size in bytes"+ msg.length+" bytes");

                return true;
            }
            return false;
        }

    }


}
