package IndyLibraries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class ProofAttributesFetched {
    public ArrayList<String> predicateCredIDs;
    ArrayList<String> PredicateCREDENTIALRevRegIDs;
      public ArrayList<String> AttrCREDENTIALRevRegIDs;
    ArrayList<String> AttrRevRegIDs;
    ArrayList<String> PredicateRevRegIDs;
    public ArrayList<String> credentialID;
    ArrayList<String> selfAttestedList;
    public ArrayList<String> AttrtoReferenceList ;
    public ArrayList<String> AttrcredDefIDs ;
    ArrayList<String> SchemaIDs ;
    public ArrayList<String> PredicatescredDefIDs;
    public ArrayList<String> predicatestoReferenceList;
    HashMap<String, Boolean> toReveal;
    public ProofAttributesFetched(
            ArrayList<String> credentialID, ArrayList<String> selfAttestedList,
            ArrayList<String> attrtoReferenceList, ArrayList<String> credDefIDsAttr,
            ArrayList<String> predicatesCredDefIdList, ArrayList<String> schemaIDs,
            ArrayList<String> credentialIDForPredicates, ArrayList<String> predicatestoReferenceList,
            ArrayList<String> AttrRevRegIDs,
            ArrayList<String> PredicateRevRegIDs, ArrayList<String> AttrCREDENTIALRevRegIDs
            , ArrayList<String> PredicateCREDENTIALRevRegIDs,
            HashMap<String, Boolean> toReveal) {
        this.credentialID = credentialID;
        this.selfAttestedList = selfAttestedList;
        this.AttrtoReferenceList = attrtoReferenceList;
        this.AttrcredDefIDs = credDefIDsAttr;

        this.SchemaIDs = schemaIDs;
        this.PredicatescredDefIDs=predicatesCredDefIdList;
        this.predicatestoReferenceList = predicatestoReferenceList;
        this.predicateCredIDs=credentialIDForPredicates;
        this.PredicateRevRegIDs=PredicateRevRegIDs;
        this.AttrRevRegIDs = AttrRevRegIDs;
        this.PredicateCREDENTIALRevRegIDs=PredicateCREDENTIALRevRegIDs;
        this.AttrCREDENTIALRevRegIDs = AttrCREDENTIALRevRegIDs;
        this.toReveal =toReveal;

    }

}
