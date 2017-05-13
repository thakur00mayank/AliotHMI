package io.datonis.aliothmi;

import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datonis.aliothmi.adapter.AdapterType;
import io.datonis.aliothmi.adapter.BaseAdapter;
import io.datonis.aliothmi.adapter.OPCUAAdapter;

/**
 * Created by mayank on 29/4/17.
 */

public class DataHandler {
    private static Logger logger = LoggerFactory.getLogger(DataHandler.class);
    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    public DataHandler(SharedPreferences preferences) {
        this.preferences = preferences;
        this.editor = preferences.edit();
    }

    public boolean getCurrentMachineStatus() {
        return preferences.getBoolean("current_machine_status", false);
    }

    public boolean setCurrentMachineStatus(Object status) {
        String machineStatusType = preferences.getString("machine_status_type", "");
        boolean previousMachineStatus = getCurrentMachineStatus();
        boolean currentMachineStatus;
        if (machineStatusType.equalsIgnoreCase("machine_status")) {
            currentMachineStatus = convertToBoolean(status);
        } else if (machineStatusType.equalsIgnoreCase("job_count")) {
            float machineCurrentJobCount = Float.parseFloat(status.toString());
            float machinePreviousJobCount = preferences.getFloat("machine_previous_job_count",
                    machineCurrentJobCount);
            currentMachineStatus = (machineCurrentJobCount != machinePreviousJobCount);
            editor.putFloat("machine_previous_job_count", machineCurrentJobCount);
        } else {
            float machineJobValue = Float.parseFloat(status.toString());
            currentMachineStatus = (machineJobValue != 0);
        }
        editor.putBoolean("current_machine_status", currentMachineStatus);
        if (previousMachineStatus == true && currentMachineStatus == false) {
            return setLastIdleSlotStartTime(System.currentTimeMillis(), 0);
        } else if (previousMachineStatus == false && currentMachineStatus == true) {
            editor.putLong("last_productive_slot_start_time", System.currentTimeMillis());
        }
        return editor.commit();
    }

    /**
     * This method will give the current reason code set in preferrences.
     * @return if current reason is set then return that else return 0.
     */
    public int getCurrentReasonCode() {
        return preferences.getInt("current_reason_code", 0);
    }

    /**
     * This method will set the given reason code to current reason code in the preferences.
     * @param reasonCode reason code to be set to current reason code in preferences.
     * @return true if successfuly set the given value to preferences.
     */
    public boolean setCurrentReasonCode(int reasonCode) {
        editor.putInt("current_reason_code", reasonCode);
        return editor.commit();
    }

    public int getCurrentOperatorCode() {
        return preferences.getInt("current_operator_code", 0);
    }

    public boolean setCurrentOperatorCode(int operatorCode) {
        editor.putInt("current_operator_code", operatorCode);
        return editor.commit();
    }

    public long getLastIdleSlotStartTime() {
        return preferences.getLong("last_idle_slot_Start_time", 0);
    }

    public boolean setLastIdleSlotStartTime(long lastIdleTime, int reasonCode) {
        editor.putLong("last_idle_slot_Start_time", lastIdleTime);
        String[] tags = {getReasonCodeTag()};
        Object[] tagValues = {reasonCode};
        new Thread(new AdapterDataWriter(tags, tagValues, getAdapter())).start();
        return editor.commit();
    }

    public long getLastProdcutiveSlotStartTime() {
        return preferences.getLong("last_productive_slot_start_time", 0);
    }

    public long getDataSyncInterval() {
        return Long.parseLong(preferences.getString("sync_frequency","1000"));
    }

    public long getIdlePopupTime() {
        return Long.parseLong(preferences.getString("idle_popup_time","240000"));
    }

    //Below methods are dependent on the server type

    public AdapterType getAdatpterType() {
        AdapterType adapterType = null;
        String adapterTypeStr = preferences.getString("server_type", "");
        if (adapterTypeStr.equalsIgnoreCase("opc_ua")) {
            adapterType = AdapterType.OPC_UA;
        } else if (adapterTypeStr.equalsIgnoreCase("opc_da")) {
            adapterType = AdapterType.OPC_DA;
        } else if (adapterTypeStr.equalsIgnoreCase("modbus")) {
            adapterType = AdapterType.MODBUS;
        } else {
            logger.error("Adapter type does not valid.");
        }
        return adapterType;
    }

    public String getReasonCodeTag() {
        String reasonCodeTag = null;
        AdapterType adapterType = getAdatpterType();
        if (adapterType == null) {
            return null;
        }
        switch (adapterType) {
            case OPC_UA:
                reasonCodeTag = preferences.getString("opc_ua_reason_code_tag", null);
                if (reasonCodeTag == null) {
                    logger.error("Tag for reason code for OPC UA not found.");
                }
                break;
            case OPC_DA:
                reasonCodeTag = preferences.getString("opc_da_reason_code_tag", null);
                if (reasonCodeTag == null) {
                    logger.error("Tag for reason code for OPC DA not found.");
                }
                break;
            case MODBUS:
                reasonCodeTag = preferences.getString("modbus_reason_code_tag", null);
                if (reasonCodeTag == null) {
                    logger.error("Tag for reason code for Modbus not found.");
                }
                break;
        }
        return reasonCodeTag;
    }

    public String getMachineStatusTag() {
        String machineStatusTag = null;
        AdapterType adapterType = getAdatpterType();
        if (adapterType == null) {
            return null;
        }
        switch (adapterType) {
            case OPC_UA:
                machineStatusTag = preferences.getString("opc_ua_machine_status_tag", null);
                if (machineStatusTag == null) {
                    logger.error("Tag for machine status for OPC UA not found.");
                }
                break;
            case OPC_DA:
                machineStatusTag = preferences.getString("opc_da_machine_status_tag", null);
                if (machineStatusTag == null) {
                    logger.error("Tag for machine status for OPC DA not found.");
                }
                break;
            case MODBUS:
                machineStatusTag = preferences.getString("modbus_machine_status_tag", null);
                if (machineStatusTag == null) {
                    logger.error("Tag for machine status for Modbus not found.");
                }
                break;
        }
        return machineStatusTag;
    }

    public BaseAdapter getAdapter() {
        BaseAdapter adapter = null;
        AdapterType adapterType = getAdatpterType();
        if (adapterType == null) {
            return null;
        }
        switch (adapterType) {
            case OPC_UA:
                String url = preferences.getString("opc_ua_server_url", "");
                boolean secure = preferences.getBoolean("opc_ua_secure_switch", true);
                adapter = new OPCUAAdapter(url, secure, false);
                break;
            case OPC_DA:
                adapter = null;
                logger.info("Adapter for OPC DA is not implemented yet.");
                break;
            case MODBUS:
                adapter = null;
                logger.info("Adapter for modbus is not implemented yet.");
                break;
        }
        return adapter;
    }

    //Private methods

    private boolean convertToBoolean(Object value) {
        boolean retVal = false;
        if (value instanceof Number) {
            retVal = (Float.parseFloat(value.toString())!=0);
        } else if (value instanceof Boolean) {
            retVal = (boolean) value;
        } else if (value instanceof String) {
            retVal = Boolean.parseBoolean((String) value);
        }
        return retVal;
    }

}
