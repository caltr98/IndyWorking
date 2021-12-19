# IndyWorking
HIghter level libraries for credential issuing and revocation and Shipping an Item use-case implementation



Shipping Simulation
5 Different processes:
Store->STOREMAINTEST
Lockerbox->LockerboxMainTest
Customer->ClienteMarket
ShippingAgent->ShippingAgentMainTest

Any Agent connection to the pool must create a Pool configuration before connecting to the pool,
in  a local environment any agent could make one that can be used by all other agents in the local machine,
Calling method createPoolLedgerConfig() from IndyLibraries.PoolUtils a configuration based on the IndyScan pool will be created,
to create a configuration an Agent needs a genesis transaction of the Pool, the method createGenesisTxnFile does that
for the IndyScan pool.
The method create PoolLedgerConfig(String poolName,String pathToGenesis) creates a pool ledger config
based on a genesis transaction file stored in the filesystem of the agent,
the genesis transaction file is usually provided to the Agent to make it able to connect to the Pool.




#Setting Up DID2DID communication
Given a local Agent and a remote Agent,any message beetween them
needs to be encrypted and signed by their privates key for security
and privacy reasons, the method that will be called to set it up will be static
methods from class DID2DID2Comm in the package IndyLibraries
Local Agent calls setupDID2DIDCommunicationAsk to create a message for setting up
their DID2DID communication,
this method will be called in the class Agent in the package IndyLibraries
currently methods that use it are:

- Agent.createDID2DIDCommunication(String agentName, InetSocketAddress agentAddress, DatagramSocket ds) for creating
a DID2DID communication using UDP message for exchanging messages beetween agents
- Agent.createDID2DIDCommunicationAsk(String agentName) creates only a message for setting up the communication


Local agent can also call askForDID2DIDCommunication from DID2DIDComm class to setup a DID2DID communication with a TCP type of connection,
using method:
- Agent.createConnection(Socket connectionSocket,String agentName)

The response will be handled by the remote agent with method
- Agent.AcceptConnection(String receivedMsg, String agentName) calling DID2DIDCommcreateDIDtoDID
  given a message of DID2DID communication request handles it and return a message (in bytes)
  with his DID to remote Agent.

Stewards can be added with a NYM transaction by a TRUSTEE or a default Steward can be specified in the 
genesis file of the Pool, in this case a new Steward is created using seed "000000000000000000000000Steward1" for DID creation,
Steward call method   indySteward.createDID(stewardSeed) to create a DID based on the provided Seed and that will 
be already a public DID in the Ledger


##Setup  Store
Store connects to Steward, knowing the address of the Socket
where Steward is ready for listening, Store and Steward setup
a DID2DID connection, Steward makes a NYM transaction towards the
Pool, writing in the Domain Ledger the public DID of Store.
Store then makes an ATTRIB Transaction, adding his endpoint where he's ready for listening to 
Customers.




###Implementation steps:
Upon received request by the Store, Steward assigns a Thread to handle the task of assign the Store a 
role in the ledger, StewardThread calls  assignEndorserRole(DIDStructure newEndorserDid,
boolean isTrustAnchor ) from IndyLibraries.StewardAgent .
Store will add his ipaddress + port with  the method addENdpointToNYM from IndyLibraries.Agent

##Customer to Store 
Customer given the desired Store DID, searches for his endpoint the
ledger, it is a read transaction so even if customer doesn't have a role/identity
in the Ledger he can do it, with a GetAttrib transaction Customer gets
the Store endpoint and connects to it.
Store and Customer setups their DID2DID communication,
Store creates a Thread to handle the Customer requests,
StoreThread sends to customer the list of available items.
Customer sends his order and waits for Store response,
if Item is still Available then StoreThread removes it from the list of available Items and proceeds
to make a shipping to an available lockerbox, store sends a credential to use for opening the lockerbox
and the endpoint of the lockerbox to customer.

###Implementation Steps:
StoreThread-1 handles request with method HandleRequest, when a Customer ask for Items a Cred_Request_0 will arrive,
then Store will use the method returnCredentialOffer(String credDefId) from IndyLibraries.Endorser to create a
credential offer,
then sends along with the credential offer the list of available items.
Customer will then create a credential request using method returnproverCreateCredentialRequest()
that calls the Indy-SDK method Anoncreds.proverCreateCredentialReq(..).
StoreThread-2 will be created to handle a Cred_Request_1, for Customer it will create a credential using
StoreIndy.returnIssuerCreateCredentialNonRevocable(CredOfferStructure credOffer) from IndyLibreries.Endorser()
that call the method Anoncreds.issuerCreateCredential(...) creating a  credential following the schema
CustomerShippingschema.


##Store to ShippingAgent
StoreThread-2 after reserving a LockerBox for an Item will then proceed to give the task of shipping it to a 
ShippingAgent,StoreThread-2 connects ShippingAgent given his ip and sends request to deliver an Item,
Store class contains Store2ShippingAgent inner class to create Objects for handling the communication with ShippingAgent
StoreThread-2 gives to the ShippingAgent a credential to open the Box and insert the Item (a credential from schema
Shipment)

###Implementation Steps:
The steps are equals to the ones for customer except that it will be created
using the  shipmentparcel schema and his attributes.


##Store to LockerBox:
Store connects to LockerBox given is ip address + port, in the process of shipping an Item to a 
customer Store tries to reserve the LockerBox for the Item, Store can use more than one Box for
shipping different Items.If LockerBox is not already reserved for another Item or it's not already occupied by an Item,
then Store provision LockerBox with the values of the parameter to insert in the proof request
to the Customer and ShippingAgent to be created by the LockerBox.

###Implementation Steps:
  Store setups a connection to Box with askForConnectionToBox() and ask to reserve Box with
  reserveBoxForItem,LockerBox handles request InsertItem using method occupyBOX(...) 

##ShippingAgent to LockerBox:
ShippingAgent deliver all the Item he has in queue to be delivered, in doing so
connects to the lockerbox specified by the store during the exchange of the credential
ShippingAgent creates a proof based on the credential given from the Store after received a 
proof request by the LockerBox, the proof request will put restrictions on the values of some
attributes of the credential.

##Implementation steps:
  ShippingAgent deliver all the Item he has in queue to be delivered, in doing so
  connects to the lockerbox specified by the store during the exchange of the credential
  Box uses method askShippingAgentForProofRequest() to ask for a proof request, this method calls
the method generateAttrInfoForProofRequest from IndyLibraries. Agent and creates the json object for the 
proof request.
ShippingAgent calls the method  returnProverSearchAttrForProof(String proofRequestJson,ArrayList<String> requestedRevealed) creates a 
proof from IndyLibraries.Agent that calls the methods CredentialsSearchForProofReq.open(..) and
credentialsSearch.fetchNextCredentials(..) to search for credential in the wallet.
With the fetched credential ShippingAgent calls
proverCreateProof(ProofAttributesFetched fetchedAttrProofs,
String proofRequestJson, String[] selfAttestedListValues
, Long timestamp,
int blobreaderhandle) from IndyLibraries.Agent, that calls the Indy-SDK method Anoncreds.proverCreateProof(..)
to create proof.
Box verifies the proof using returnVerifierVerifyProofNOREVOCATION(String proofRequestJson, String proofJson, String schemas,
String credentialDefs) from IndyLibraries.Agent and using Anoncreds.verifierVerifyProof(..) method.
After verifying the proof if it's valid LockerBox also starts a timer from currenttime=the time of receiving the proof
to currenttime+shipmentavailabilitytime (Attribute from proof that must be in the revealed form).

##Customer to Box
Customer connects to box and if box is not empty(meeanign that ShippingAgent has done his job before) then Customer can present his
proof to LockerBox, if proof is valid then LockerBox checks that it arrived in the timeframe where shipment is availabile to Customer.

###Implementation Steps
  Steps are similar to those of ShippingAgent to Box,except for the check
of opening time after providing the proof to LockerBox.







