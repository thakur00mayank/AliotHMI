package io.datonis.aliothmi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datonis.aliothmi.adapter.BaseAdapter;

/**
 * Created by mayank on 7/5/17.
 */

public class AdapterDataWriter implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(AdapterDataWriter.class);
    String[] tags;
    Object[] tagValues;
    BaseAdapter adapter;


    public AdapterDataWriter(String[] tags, Object[] tagValues, BaseAdapter adapter) {
        this.tags = tags;
        this.tagValues = tagValues;
        this.adapter = adapter;
    }

    @Override
    public void run() {
        if (adapter == null) {
            logger.info("No Adapter Found to perform service task. Killing service.");
            return;
        }
        adapter.connect();
        adapter.writeTagValues(tags, tagValues);

        adapter.shutdown();;
    }
}
