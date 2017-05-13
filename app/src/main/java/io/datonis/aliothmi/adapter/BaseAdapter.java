package io.datonis.aliothmi.adapter;

/**
 * Created by mayank on 29/4/17.
 */

public abstract class BaseAdapter {

    public abstract void connect();

    public abstract void shutdown();

    public abstract Object[] readTagValues(String tags[]);

    /**
     * This class will write the given values to respective tag.
     * Each tag should have its respective value in values object array.
     * @param tags String array containing tag info.
     * @param values Values with respect to tags
     */
    public abstract void writeTagValues(String tags[], Object values[]);
}
