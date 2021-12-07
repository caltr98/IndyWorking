package IndyLibraries;

public class CredOfferStructure {
    public CredDefStructure credDef;
    public String credOffer;
    public String revRegId;

    public CredOfferStructure(CredDefStructure credDef, String credOffer) {
        this.credDef = credDef;
        this.credOffer = credOffer;
    }
    public CredOfferStructure(CredDefStructure credDef, String credOffer, String revRegId) {
        this.credDef = credDef;
        this.credOffer = credOffer;
        this.revRegId=revRegId;
    }

}
