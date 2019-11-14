package com.letrans.android.translator.lyy;

import com.letrans.android.translator.utils.Logger;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class TranslatePool {
    private static final String TAG = "RTranslator/TranslatePool";

    private static GenericObjectPool<TranslateWork> objectPool = null;

    static {
        // 连接池的配置
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        // 池中的最大连接数
        poolConfig.setMaxTotal(8);
        // 最少的空闲连接数
        poolConfig.setMinIdle(0);
        // 最多的空闲连接数
        poolConfig.setMaxIdle(8);
        // 当连接池资源耗尽时,调用者最大阻塞的时间,超时时抛出异常 单位:毫秒数
        poolConfig.setMaxWaitMillis(1000 * 15);
        // 连接池存放池化对象方式,true放在空闲队列最前面,false放在空闲队列最后
        poolConfig.setLifo(true);
        // 连接空闲的最小时间,达到此值后空闲连接可能会被移除,默认即为30分钟
//        poolConfig.setMinEvictableIdleTimeMillis(1000L * 60L * 15L);
        poolConfig.setSoftMinEvictableIdleTimeMillis(1000L * 60L * 9L);

        // 连接耗尽时是否阻塞,默认为true
        poolConfig.setBlockWhenExhausted(true);

        poolConfig.setJmxEnabled(false);

        poolConfig.setTestOnBorrow(true);//向调用者输出“链接”资源时，是否检测是有有效，如果无效则从连接池中移除，并尝试获取继续获取。默认为false。建议保持默认值.
        poolConfig.setTestWhileIdle(true);//向调用者输出“链接”对象时，是否检测它的空闲超时；默认为false。如果“链接”空闲超时，将会被移除。建议保持默认值.
        poolConfig.setTimeBetweenEvictionRunsMillis(1000 * 60);// “空闲链接”检测线程，检测的周期，毫秒数。如果为负值，表示不运行“检测线程”。默认为-1.

        // 连接池创建
        objectPool = new GenericObjectPool<>(new TranslatFactory(), poolConfig);
    }

    private static TranslateWork borrowObject() {
        try {
            TranslateWork clientSingle = objectPool.borrowObject();
            Logger.i(TAG, "总创建对象数" + objectPool.getCreatedCount());
            return clientSingle;
        } catch (Exception e) {
            e.printStackTrace();
        }
        //连接池失败则主动创建
        return createClient();
    }

    public static void clearObject() {
        objectPool.clear();
    }

    /**
     * 当连接池异常,则主动创建对象
     */
    private static TranslateWork createClient() {
        return new TranslateWork("222.186.36.150", 55001);
    }

    /**
     * 执行器
     *
     * @param workCallBack 主要服务内容
     */
    public static Runnable execute(WorkCallBack<TranslateWork> workCallBack) {
        return new Runnable() {
            @Override
            public void run() {
                TranslateWork client = borrowObject();
                client.checkChannelState();
                try {
                    workCallBack.callback(client);
                } finally {
                    /** 将连接对象返回给连接池 */
                    objectPool.returnObject(client);
                }
            }
        };
    }
}
