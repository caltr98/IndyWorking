import IndyLibraries.DIDStructure;
import IndyLibraries.JSONUserCredentialStorage;
import IndyLibraries.StewardAgent;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.pool.Pool;
import org.json.JSONObject;

import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MarketPlaceSteward {
    String trusteeSeed;
    StewardAgent indySteward;
    ServerSocketChannel AgentListenChannel;
    InetSocketAddress listeningAddress;
    String StewardName;
    public MarketPlaceSteward(String poolName){//default Steward
        String stewardSeed = "000000000000000000000000Steward1";
        Pool pool=null;
        StewardName = "Default Steward";
        JSONUserCredentialStorage jsonStoredCred = null;
        try {
            Pool.setProtocolVersion(2).get();
            poolName = IndyLibraries.PoolUtils.createPoolLedgerConfig();
            pool = Pool.openPoolLedger(poolName, "{}").get();
            AgentListenChannel = ServerSocketChannel.open();
            AgentListenChannel.socket().bind(null);
            listeningAddress = (InetSocketAddress) AgentListenChannel.getLocalAddress();
            System.out.println("IndyLibraries.Agent default Steward Listening Adress:" + listeningAddress);

            File agentsFile = new File("./" + "mainSteward" + ".json");

            jsonStoredCred = new JSONUserCredentialStorage(agentsFile);
        } catch (IndyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        indySteward = new StewardAgent(pool,"mainSteward",jsonStoredCred);
        indySteward.CreateWallet("mainSteward", "pass");
        indySteward.OpenWallet("mainSteward", "pass");
        System.out.println("MarketPlaceDID " +
        indySteward.createDID(stewardSeed));
        //add endpoint to NYM Object in Ledger to talk with Steward
        //inserting address 127.0.0.1 for localhost
        indySteward.addENdpointToNYM("did2did","127.0.0.1:"+listeningAddress.getPort());
        Thread th = new Thread(new marketPlaceStewardRequestsHandler(pool,AgentListenChannel,this));
        System.out.println("gettingPoolStats" + indySteward.GetvalidatorInfo());
        th.start();
    }

    public String acceptConnection(Socket connectionSocket, String receivedMSG){
        //return the message of connection accepted with own DID2DID  comunication information
        String acceptedconn=indySteward.acceptConnection(receivedMSG,StewardName);
        return acceptedconn;
    }
    public class marketPlaceStewardTaskHandler implements Runnable{
        private byte[] receivedRequest;
        private MarketPlaceSteward marketPlaceSteward;
        private SocketChannel agentChannelToWrite;
        private boolean clientReady;
        private ReentrantLock lockState;//lock access shared variables with RequestHandler thread
        private Condition waitForTask;//condition variable to use while waiting to write answer

        public marketPlaceStewardTaskHandler(byte[] receivedRequest, String s, MarketPlaceSteward marketPlaceSteward) {
            lockState=new ReentrantLock();
            waitForTask=lockState.newCondition();
            clientReady=false;
            this.receivedRequest=receivedRequest;
            this.marketPlaceSteward = marketPlaceSteward;

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
            response = marketPlaceSteward.HandleRequest(request)+"\n";
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

    private String HandleRequest(byte[] request) {
        System.out.println("in handle request");
        String regexNospace = "([a-zA-Z0-9.~!$%^&*_=+_%\\[\\]()\"{}\\\\:;,^?-]+)"; // match only symbols
        String unpacked_request = indySteward.readMessage(request);
        JSONObject requestJSON=new JSONObject(unpacked_request);
        String returnMessage= unpacked_request;
        if(requestJSON.get("request").equals("NYMENDORSER")){
            requestJSON=requestJSON.getJSONObject("fields");
            returnMessage = String.valueOf(this.indySteward.assignEndorserRole(new DIDStructure(requestJSON.getString("did"),requestJSON.getString("verkey")),
                    requestJSON.getString("trustAnchor").equals("true"))) +" assigned role";
        }
        return returnMessage;
    }

    private class marketPlaceStewardRequestsHandler implements Runnable{
        ServerSocketChannel AgentListenChannel;//serverSocketChannel per il multiplexing dei canali
        Pool pool;
        int foreignAgentID;
        MarketPlaceSteward marketPlaceSteward;
        private ArrayList<String> foreignAGENTDID;

        public marketPlaceStewardRequestsHandler(Pool pool,ServerSocketChannel AgentListenChannel,
                                                 MarketPlaceSteward marketPlaceSteward){
            this.pool = pool;
            this.AgentListenChannel = AgentListenChannel;
            foreignAgentID=0;
            this.marketPlaceSteward = marketPlaceSteward;
        }

        @Override
        public void run() {
            byte[] tmpTOString;
            Selector selector = null;
            Set<SelectionKey> readKeys;
            Iterator<SelectionKey> keyIterator;
            ByteBuffer byteBuffer;
            ArrayList<Byte> growingBuffer = new ArrayList<Byte>();
            HashMap<Integer, MarketPlaceSteward.marketPlaceStewardTaskHandler> clientsWorker=new HashMap<>();//Store in HashMap AgentID->AgentHandler
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
                            MarketPlaceSteward.marketPlaceStewardTaskHandler handler;
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
                                    foreignAGENTDID.put(id,marketPlaceSteward.acceptConnection(ForeignAgent.socket(),receivedString));
                                } else if (receivedString.equals("DISCONNECT")) {//Logout totalmente effettuato nel MainThread-Server
                                    foreignAGENTDID.remove(id);//remove the DID of the Foreign from the Know DID list
                                    key.cancel();
                                    key.channel().close();//closed channel with ForeignAgent


                                } else {

                                    System.out.println("created an handler for a request of IndyLibraries.Agent: " + foreignAGENTDID.get(id));
                                    //we need the byte
                                    handler =
                                            this.marketPlaceSteward.new marketPlaceStewardTaskHandler(tmpTOString,
                                                    foreignAGENTDID.get(id),
                                            this.marketPlaceSteward);
                                    clientsWorker.put(id, handler);
                                    taskToAgentExecutor.submit(handler);

                                }
                            }
                        } else if (key.isWritable()) {//Quando il Client è pronto a ricevere risposta
                            int id = (int) key.attachment();
                            MarketPlaceSteward.marketPlaceStewardTaskHandler handler;
                            if (clientsWorker.containsKey(id)) {//SE non è presenta allora vuol dire che non
                                //si è letta richiesta in precedenza
                                System.out.println("can give answer");
                                SocketChannel client = (SocketChannel) key.channel();
                                handler = clientsWorker.remove(id);//get and remove handler reference
                                handler.canGiveAnswer(client);//give channel to write answer
                                key.interestOps(SelectionKey.OP_READ);//set  OP_READ in interestSet
                                //wait new request from this client
                            }
                            else {//if there is no handler assigned to this agent then this is the  did2did communication setup phase

                                String[] DID2DIDSetupData=foreignAGENTDID.get(id).split("HANDSHAKE2");
                                //splitting in a convenientway the did and the rest
                                System.out.println(foreignAGENTDID.get(id));
                                JSONObject jsonObjectSetup = new JSONObject(foreignAGENTDID.get(id));
                                foreignAGENTDID.put(id,jsonObjectSetup.getString("theirDID"));
                                key.channel().configureBlocking(false);
                                ByteBuffer toSendBuf = ByteBuffer.wrap(jsonObjectSetup.getString("message").getBytes(StandardCharsets.UTF_8));
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
                                foreignAGENTDID.put(id,DID2DIDSetupData[0]);
                                key.interestOps(SelectionKey.OP_READ);//imposto operazione di OP_READ in interestSet

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

}
