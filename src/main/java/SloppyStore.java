import IndyLibraries.CreateCredentialResult;
import IndyLibraries.CredOfferStructure;
import IndyLibraries.JSONUserCredentialStorage;
import org.hyperledger.indy.sdk.pool.Pool;
import org.json.JSONObject;

import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
//the SloppyStore sends both the credential to deliver the items to the ShippingDeliverer and the
//credential to open the box for the customer.

public class SloppyStore extends Store{
    public SloppyStore(Pool handle, String StoreDIDseed, String StoreName, String storeWallet, String walletPassword) {
        super(handle, StoreDIDseed, StoreName, storeWallet, walletPassword);
    }

    public SloppyStore(String poolName, String storeName, String walletPassword) {
        super(poolName, storeName, walletPassword);
    }

    public SloppyStore(String poolName, String storeName, String didValue, String walletPassword) {
        super(poolName, storeName, didValue, walletPassword);
    }
    @Override
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
            String credReqMetadata = requestJSON.getString("credReqMetadata");
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
                            System.out.println("Box was full,type Exit to abort operation");

                            ip=scanner.next();
                            if (ip.equals("Exit")){
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
                CredOfferStructure credOffer2= StoreIndy.returnCredentialOffer(shippingCredential.credDefId);
                System.out.println("cred def format accepted by Ledger" + credOffer2.credDef.credDefId);
                store2shippingAgent.giveShipmentToAgent( new String[]{ String.valueOf(currentshipmentid),(int)((Math.
                                random()*79 )+1 )+"kg",(int)((Math.
                                random()*999 )+1 )+"euros"
                                ,nonceForShipment,
                                String.valueOf(((int) (Math.random()*71) +1))},
                        useBOX.boxAddress.getAddress().getHostAddress(),useBOX.boxAddress.getPort(),
                        new JSONObject().put("credential",ret.credentialJson).put("credreqmetadata",credReqMetadata).
                                put("credeDefId",credOffer2.credDef.credDefId).put("BoxAddress",useBOX.boxAddress.toString()).toString());

                System.out.println("Store to Customer credential size in bytes"+ ret.credentialJson.getBytes(StandardCharsets.UTF_8).length);


                System.out.println("Post Store  sending Item metrics:\n"+StoreIndy.collectMetrics());

                return new JSONObject().put("credential",ret.credentialJson).put("BoxAddress",useBOX.boxAddress.toString()).toString();

            }
        }

        System.out.println("handling request3");
        return returnMessage;

    }


}
