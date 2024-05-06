package com.generac.ces.systemgateway.model.enums;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.generac.ces.systemgateway.model.helpers.DeviceSettingDeserializer;

@JsonDeserialize(using = DeviceSettingDeserializer.class)
public enum DeviceSetting {
    UNKNOWN("unknown", "unknown"),
    BATTERY_STATE("battery_ena", "batteryEnabled"),
    AH_RTG("ah_rtg", "ahRtg"),
    WH_RTG("wh_rtg", "whRtg"),
    W_CHA_MAX("w_cha_max", "wChaMax"),
    W_DISCHA_MAX("w_discha_max", "wDisChaMax"),
    SOC_MAX("soc_max", "socMax"),
    SOC_MIN("soc_min", "socMin"),
    SOC_RSV_MAX("soc_rsv_max", "socRsvMax"),
    SOC_RSV_MIN("soc_rsv_min", "socRsvMin"),
    A_CHA_MAX("a_cha_max", "aChaMax"),
    A_DISCHA_MAX("a_discha_max", "aDisChaMax"),
    STATE("conn_inverter_enabled", "isInverterEnabled"),
    ISLANDING("islanding_enabled", "isIslandingEnabled"),
    EXPORT_OVERRIDE("export_override", "zeroExportOverride"),
    NUMBER_OF_TRANSFER_SWITCHES("xfrena_external_transfer_switches", "noOfTransferSwitches"),
    LOAD_SHEDDING("load_shed_enable_setting", "loadSheddingSetting"),
    GENERATOR_CONTROL_MODE("generator_control_mode", "acGeneratorControlMode"),
    SELF_SUPPLY_SOURCE_POWER_LIMIT(
            "ss_src_self_supply_source_power_limit_w", "selfSupplySourcePowerLimit"),
    SELF_SUPPLY_SINK_POWER_LIMIT(
            "ss_sink_self_supply_sink_power_limit_w", "selfSupplySinkPowerLimit"),
    CT_TURNS_RATIO("ct_turns_ratio", "ctTurnsRatio"),
    GRID_PARALLEL_INVERTERS("grid_parallel_inverters", "noOfACParallelInverter"),
    GENERATOR_POWER_RATING("generator_power_rating_kw", "acGeneratorPowerRating"),
    EXPORT_POWER_LIMIT("zexplim_export_power_limit_w", "maxExportPower"),
    ZERO_IMPORT("zimp_zero_import_enabled", "isZeroImportEnabled"),
    EXPORT_LIMITING("zexp_zero_export_enabled", "isZeroExportEnabled"),
    MODE("mode", "mode"),
    ACTIVE_MODES("active_modes", "activeModes"),
    PVLINK_STATE("state", "pvLinkState"),
    VIN_STARTUP("vin_startup", "vinStartup"),
    ENABLE_PVRSS("enable_pvrss", "enablePVRSS"),
    PLM_CHANNEL("plm_channel", "plmChannel"),
    NUM_STRING("num_strings", "numStrings"),
    SNAP_RS_INSTALLED_CNT("snap_rs_installed_cnt", "snapRsInstalledCnt"),
    SNAP_RS_DETECTED_CNT("snap_rs_detected_cnt", "snapRsDetectedCnt"),
    OVERRIDE_PVRSS("override_pvrss", "overridePVRSS"),
    CT_CALIBRATION("ct_trig", "ctCalibration");

    private final String rawSettingName;
    private final String formattedSettingName;

    DeviceSetting(String rawSettingName, String formattedSettingName) {
        this.rawSettingName = rawSettingName;
        this.formattedSettingName = formattedSettingName;
    }

    public String getRawSettingName() {
        return rawSettingName;
    }

    public String getFormattedSettingName() {
        return formattedSettingName;
    }

    public static DeviceSetting getByFormattedSettingName(String formattedSettingName) {
        for (DeviceSetting setting : DeviceSetting.values()) {
            if (setting.formattedSettingName.equals(formattedSettingName)) {
                return setting;
            }
        }
        return UNKNOWN;
    }

    public static DeviceSetting fromRawSettingName(String rawSettingName) {
        for (DeviceSetting setting : DeviceSetting.values()) {
            if (setting.rawSettingName.equals(rawSettingName)) {
                return setting;
            }
        }
        return UNKNOWN;
    }
}
