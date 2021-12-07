import static java.lang.System.currentTimeMillis;

public class Item {
    public String name;
    public String DID;
    private String uniqueItemIdentifier;
    public Item (String name){
        this.name = name;
        this.uniqueItemIdentifier = String.valueOf(currentTimeMillis());
    }
    public Item ( String name,String DID){
        this.name=name;
        this.DID= DID;
        this.uniqueItemIdentifier = String.valueOf(currentTimeMillis());

    }
}
