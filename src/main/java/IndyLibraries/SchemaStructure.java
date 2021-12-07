package IndyLibraries;

public class SchemaStructure {
    public String schemaId;
    public String schemaName;
    public String schemaVersion;
    public String schemaJson;
    public String[]schemaAttrs;
    public SchemaStructure(String schemaId,String schemaName,
            String schemaJson,   String[]schemaAttrs,
                           String schemaVersion){
        this.schemaName=schemaName;
        this.schemaId=schemaId;
        this.schemaVersion=schemaVersion;
        this.schemaJson=schemaJson;
        this.schemaAttrs=schemaAttrs;
    }
}
