package IndyLibraries;

import org.hyperledger.indy.sdk.blob_storage.BlobStorageReader;

public class RevocationRegistryObject {
    public String revRegId,revRegEntryJson,revRegDefJson;
    BlobStorageReader blobStorageReaderCfg;
    public int blobStorageReaderHandle;
    public RevocationRegistryObject(String revRegId, String revRegEntryJson, String revRegDefJson, BlobStorageReader blobStorageReaderCfg, int blobStorageReaderHandle) {

        this.revRegId = revRegId;
        this.revRegEntryJson = revRegEntryJson;
        this.revRegDefJson = revRegDefJson;
        this.blobStorageReaderCfg = blobStorageReaderCfg;
        this.blobStorageReaderHandle = blobStorageReaderHandle;
    }
}
