package com.letrans.android.translator.lyy;

import com.letrans.android.translator.utils.Logger;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class TranslatFactory extends BasePooledObjectFactory<TranslateWork> {
    private static final String TAG = "RTranslator/TranslatFactory";

    private String host = "222.186.36.150";
    private int port = 50051;

    @Override
    public TranslateWork create() throws Exception {
        Logger.i(TAG, "TranslatFactory create");
        return new TranslateWork(host, port);
    }

    @Override
    public PooledObject<TranslateWork> wrap(TranslateWork translator) {
        Logger.i(TAG, "TranslatFactory wrap");
        return new DefaultPooledObject<>(translator);
    }

    @Override
    public void destroyObject(PooledObject<TranslateWork> p) throws Exception {
        Logger.i(TAG, "TranslatFactory destroyObject");
        p.getObject().shutdown();
        super.destroyObject(p);
    }

    @Override
    public boolean validateObject(PooledObject<TranslateWork> p) {
        ManagedChannel channel = p.getObject().getChannel();
        ConnectivityState state = channel.getState(true);
        Logger.i(TAG, "TranslatFactory validateObject state="+state);

        if (state == ConnectivityState.TRANSIENT_FAILURE
                || state == ConnectivityState.SHUTDOWN) {
            return false;
        }
        return super.validateObject(p);
    }
}
