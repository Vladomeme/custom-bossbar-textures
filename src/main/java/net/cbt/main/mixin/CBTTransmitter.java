package net.cbt.main.mixin;

import net.cbt.main.CBTClient;

public interface CBTTransmitter {

    CBTClient cbt$getClient();

    void cbt$setClient(CBTClient client);

}
