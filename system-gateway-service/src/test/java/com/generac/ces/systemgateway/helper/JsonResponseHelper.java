package com.generac.ces.systemgateway.helper;

public class JsonResponseHelper {
    public static final String DEVICE_SETTING_RESPONSE_BATTERIES =
            """
            {
                "hostRcpn": "00010007A42A",
                "devices": [
                    {
                        "deviceId": "000100080A99",
                        "deviceType": "BATTERY",
                        "metadata": [
                            {
                                "name": "A_CHA_MAX",
                                "label": "Maximum Charge Current",
                                "value": 10.0,
                                "constraints": {
                                    "minValue": 0.0,
                                    "maxValue": 35.0,
                                    "allowedValues": [1.0, 2.0, 3.0]
                                },
                                "type": "FLOAT",
                                "unit": "AMPS",
                                "description": "Instantaneous maximum DC charge current."
                            },
                            {
                                "name": "A_DISCHA_MAX",
                                "label": "Maximum Discharge Current",
                                "value": 35.0,
                                "constraints": {
                                    "minValue": 0.0,
                                    "maxValue": 35.0,
                                    "allowedValues": [1.0, 2.0, 3.0]
                                },
                                "type": "FLOAT",
                                "unit": "AMPS",
                                "description": "Instantaneous maximum DC discharge current."
                            },
                            {
                                "name": "AH_RTG",
                                "label": "Charge Capacity",
                                "value": 62.5,
                                "constraints": {
                                    "minValue": 0.0,
                                    "maxValue": 35.0,
                                    "allowedValues": [1.0, 2.0, 3.0]
                                },
                                "type": "FLOAT",
                                "unit": "PERCENT",
                                "description": "Nameplate charge capacity in amp-hours."
                            },
                            {
                                "name": "SOC_MAX",
                                "label": "Maximum SOC",
                                "value": 99.0,
                                "constraints": {
                                    "minValue": 0.0,
                                    "maxValue": 35.0,
                                    "allowedValues": [1.0, 2.0, 3.0]
                                },
                                "type": "FLOAT",
                                "unit": "PERCENT",
                                "description": "Manufacturer maximum state of charge, expressed as a percentage."
                            },
                            {
                                "name": "SOC_MIN",
                                "label": "Minimum SOC",
                                "value": 1.0,
                                "constraints": {
                                    "minValue": 0.0,
                                    "maxValue": 35.0,
                                    "allowedValues": [1.0, 2.0, 3.0]
                                },
                                "type": "FLOAT",
                                "unit": "PERCENT",
                                "description": "Manufacturer minimum state of charge, expressed as a percentage."
                            },
                            {
                                "name": "SOC_RSV_MAX",
                                "label": "Maximum Reserve Capacity",
                                "value": 100.0,
                                "constraints": {
                                    "minValue": 0.0,
                                    "maxValue": 35.0,
                                    "allowedValues": [1.0, 2.0, 3.0]
                                },
                                "type": "FLOAT",
                                "unit": "PERCENT",
                                "description": "Setpoint for maximum reserve for storage as a percentage of the nominal maximum storage."
                            },
                            {
                                "name": "SOC_RSV_MIN",
                                "label": "Minimum Reserve Capacity",
                                "value": 1.0,
                                "constraints": {
                                    "minValue": 0.0,
                                    "maxValue": 35.0,
                                    "allowedValues": [1.0, 2.0, 3.0]
                                },
                                "type": "FLOAT",
                                "unit": "PERCENT",
                                "description": "Setpoint for minimum reserve for storage as a percentage of the nominal maximum storage."
                            },
                            {
                                "name": "W_CHA_MAX",
                                "label": "Maximum Charge Rate",
                                "value": 27642.0,
                                "constraints": {
                                    "minValue": 0.0,
                                    "maxValue": 35.0,
                                    "allowedValues": [1.0, 2.0, 3.0]
                                },
                                "type": "INTEGER",
                                "unit": "WATTS",
                                "description": "Maximum rate of energy transfer into the storage device in DC watts."
                            },
                            {
                                "name": "W_DISCHA_MAX",
                                "label": "Maximum Discharge Rate",
                                "value": 64554.0,
                                "constraints": {
                                    "minValue": 0.0,
                                    "maxValue": 35.0,
                                    "allowedValues": [1.0, 2.0, 3.0]
                                },
                                "type": "INTEGER",
                                "unit": "WATTS",
                                "description": "Maximum rate of energy transfer out of the storage device in DC watts."
                            },
                            {
                                "name": "WH_RTG",
                                "label": "Energy Capacity",
                                "value": 17550.0,
                                "constraints": {
                                    "minValue": 0.0,
                                    "maxValue": 35.0,
                                    "allowedValues": [1.0, 2.0, 3.0]
                                },
                                "type": "INTEGER",
                                "unit": "WATT_HOURS",
                                "description": "Nameplate energy capacity in DC watt-hours."
                            }
                        ]
                    }
                ]
            }
            """;

    public static final String DEVICE_SETTING_RESPONSE_INVERTERS =
            """
            {
                "hostRcpn": "000100080A99",
                "devices": [
                    {
                        "deviceId": "000100080A99",
                        "deviceType": "INVERTER",
                        "metadata": [
                            {
                                "name": "STATE",
                                "label": "Enable/Disable Inverter",
                                "value": 1.0,
                                "constraints": {
                                    "minValue": 0.0,
                                    "maxValue": 35.0,
                                    "allowedValues": [1.0, 2.0, 3.0]
                                },
                                "type": "BOOLEAN",
                                "unit": "KILOWATTS",
                                "description": "This setting allows users to remotely disable and re-enable inverter"
                            },
                            {
                                "name": "ISLANDING",
                                "label": "Enable Islanding",
                                "value": 0.0,
                                "constraints": {
                                    "minValue": 0.0,
                                    "maxValue": 35.0,
                                    "allowedValues": [1.0, 2.0, 3.0]
                                },
                                "type": "BOOLEAN",
                                "unit": "KILOWATTS",
                                "description": "Allows system to island, providing backup power during a grid outage"
                            },
                            {
                                "name": "EXPORT_OVERRIDE",
                                "label": "Export Override",
                                "value": 1.0,
                                "constraints": {
                                    "minValue": 0.0,
                                    "maxValue": 35.0,
                                    "allowedValues": [1.0, 2.0, 3.0]
                                },
                                "type": "BOOLEAN",
                                "unit": "KILOWATTS",
                                "description": "Inhibits PWRcell system from exporting power to the grid"
                            },
                            {
                                "name": "NUMBER_OF_TRANSFER_SWITCHES",
                                "label": "Number of Transfer Switches",
                                "value": 5.0,
                                "constraints": {
                                    "minValue": 0.0,
                                    "maxValue": 35.0,
                                    "allowedValues": [1.0, 2.0, 3.0]
                                },
                                "type": "INTEGER",
                                "unit": "KILOWATTS",
                                "description": "Set to zero if no ATS, set to 1 if a single external ATS is installed and set to 2 if two ATSs are installed to operate with the inverter"
                            },
                            {
                                "name": "LOAD_SHEDDING",
                                "label": "Enable Load Shedding",
                                "value": 1.0,
                                "constraints": {
                                    "allowedValues": [
                                        {
                                            "name": "LOAD_SHED_SMM_ONLY",
                                            "value": 1.0
                                        },
                                        {
                                            "name": "LOAD_SHED_DISABLED",
                                            "value": 0.0
                                        },
                                        {
                                            "name": "LOAD_SHED_ATS_AND_SMM",
                                            "value": 2.0
                                        }
                                    ]
                                },
                                "type": "ENUM",
                                "unit": "KILOWATTS",
                                "description": "Select 1 if using SMM/PWRManager devices to shed loads, 2 if using PWRCell SACM to shed loads (with or without SMM)"
                            },
                            {
                                "name": "GENERATOR_CONTROL_MODE",
                                "label": "AC Generator control mode",
                                "value": 0.0,
                                "constraints": {
                                    "allowedValues": [
                                        {
                                            "name": "ALWAYS_ON",
                                            "value": 2.0
                                        },
                                        {
                                            "name": "SOURCE_CYCLING",
                                            "value": 1.0
                                        },
                                        {
                                            "name": "SINGLE_TRANSFER",
                                            "value": 0.0
                                        }
                                    ]
                                },
                                "type": "ENUM",
                                "unit": "KILOWATTS",
                                "description": "An AC Generator integrated into an ESS PWRCell can operate under one of 3 control modes; 0 - 'Single Transfer', 1 - 'Source Cycling', or 2 - 'Always on'"
                            },
                            {
                                "name": "SELF_SUPPLY_SOURCE_POWER_LIMIT",
                                "label": "Self Supply Source Power",
                                "value": 12.0,
                                "constraints": {
                                    "minValue": 0.0,
                                    "maxValue": 35.0,
                                    "allowedValues": [1.0, 2.0, 3.0]
                                },
                                "type": "INTEGER",
                                "unit": "KILOWATTS",
                                "description": "Max threshold for importing power before battery discharges in self supply mode"
                            },
                            {
                                "name": "SELF_SUPPLY_SINK_POWER_LIMIT",
                                "label": "Self Supply Sink Power",
                                "value": 50.0,
                                "constraints": {
                                    "minValue": 0.0,
                                    "maxValue": 35.0,
                                    "allowedValues": [1.0, 2.0, 3.0]
                                },
                                "type": "INTEGER",
                                "unit": "KILOWATTS",
                                "description": "Min power import maintained by charging the battery from the grid in self supply mode"
                            },
                            {
                                "name": "CT_TURNS_RATIO",
                                "label": "CT Turns Ratio",
                                "value": 45.0,
                                "constraints": {
                                    "minValue": 0.0,
                                    "maxValue": 35.0,
                                    "allowedValues": [1.0, 2.0, 3.0]
                                },
                                "type": "INTEGER",
                                "unit": "KILOWATTS",
                                "description": "Allows a different turns ratio to be set for the specific CT"
                            },
                            {
                                "name": "GRID_PARALLEL_INVERTERS",
                                "label": "GridParInverters",
                                "value": 2.0,
                                "constraints": {
                                    "minValue": 0.0,
                                    "maxValue": 35.0,
                                    "allowedValues": [1.0, 2.0, 3.0]
                                },
                                "type": "INTEGER",
                                "unit": "KILOWATTS",
                                "description": "This setting allows for two inverters to share one set of CTs. Set to 2 if daisy chaining CTs between two inverters"
                            },
                            {
                                "name": "GENERATOR_POWER_RATING",
                                "label": "GenPower kW",
                                "value": 100.0,
                                "constraints": {
                                    "minValue": 0.0,
                                    "maxValue": 35.0,
                                    "allowedValues": [1.0, 2.0, 3.0]
                                },
                                "type": "FLOAT",
                                "unit": "KILOWATTS",
                                "description": "test description"
                            },
                            {
                                "name": "EXPORT_POWER_LIMIT",
                                "label": "Export Limit",
                                "value": 100.0,
                                "constraints": {
                                    "minValue": 0.0,
                                    "maxValue": 35.0,
                                    "allowedValues": [1.0, 2.0, 3.0]
                                },
                                "type": "INTEGER",
                                "unit": "KILOWATTS",
                                "description": "Set the kW limit exported to grid"
                            },
                            {
                                "name": "ZERO_IMPORT",
                                "label": "Zero Import",
                                "value": 0.0,
                                "constraints": {
                                    "minValue": 0.0,
                                    "maxValue": 35.0,
                                    "allowedValues": [1.0, 2.0, 3.0]
                                },
                                "type": "BOOLEAN",
                                "unit": "KILOWATTS",
                                "description": "Enable / disable zero import"
                            }
                        ]
                    }
                ]
            }
            """;

    public static final String BATTERY_PROPERTIES_RESPONSE =
            """
            {
                "deviceId": "000100080A99",
                "timestampUtc": "2023-07-24T20:09:16Z",
                "type": 4,
                "ahRtg": 62.5,
                "whRtg": 17550.0,
                "socMax": 100.0,
                "socMin": 1.0,
                "socRsvMax": 100.0,
                "socRsvMin": 1.0,
                "w": -5.0,
                "i": -0.01,
                "v": 312.2,
                "soc": 95.0,
                "soh": 94.0,
                "maxCellV": 4.0,
                "minCellV": 3.984,
                "wChaMax": 64989.0,
                "wDischaMax": 64554.0,
                "aChaMax": 30.0,
                "aDischaMax": 35.0
            }
            """;

    public static final String DEVICE_SETTING_RESPONSE_PVLS =
            """
            {
                "hostDeviceId": "000100080A99",
                         "devices": [
                             {
                                 "deviceId": "000100080A99",
                                 "deviceType": "PVLINK",
                                 "settings": [
                                     {
                                         "name": "VIN_STARTUP",
                                         "label": "Vin Startup",
                                         "value": 1110.0,
                                         "constraints": {
                                             "minValue": 60.0,
                                             "maxValue": 420.0
                                         },
                                         "type": "INTEGER",
                                         "unit": "VOLTS",
                                         "description": "Minimum input voltage from PV sub-string for the PV Link to make power",
                                         "updatedTimestampUtc": "2023-12-19T20:45:24.818896Z"
                                     },
                                     {
                                         "name": "SNAP_RS_DETECTED_CNT",
                                         "label": "SnapRSDetected",
                                         "value": 0.0,
                                         "constraints": {},
                                         "type": "INTEGER",
                                         "description": "The number of SnapRS devices detected by the PV Link after its daily count.",
                                         "updatedTimestampUtc": "2023-12-19T20:45:28.789391Z"
                                     },
                                     {
                                         "name": "SNAP_RS_INSTALLED_CNT",
                                         "label": "SnapRSInstalled",
                                         "value": 0.0,
                                         "constraints": {
                                             "minValue": 0.0,
                                             "maxValue": 12.0
                                         },
                                         "type": "INTEGER",
                                         "description": "The total number of SnapRS devices physically installed on this PV Link.",
                                         "updatedTimestampUtc": "2023-12-19T20:45:20.962891Z"
                                     },
                                     {
                                         "name": "NUM_STRING",
                                         "label": "String Count",
                                         "value": 1.0,
                                         "constraints": {
                                             "allowedValues": [
                                                 {
                                                     "name": "ONE_SUBSTRING",
                                                     "value": 1.0
                                                 },
                                                 {
                                                     "name": "TWO_SUBSTRINGS",
                                                     "value": 2.0
                                                 }
                                             ]
                                         },
                                         "type": "ENUM",
                                         "description": "Number of parallel PV substrings connected to this PV Link.",
                                         "updatedTimestampUtc": "2023-12-19T20:46:10.369715Z"
                                     },
                                     {
                                         "name": "ENABLE_PVRSS",
                                         "label": "Enable PVRSS",
                                         "value": 0.0,
                                         "constraints": {},
                                         "type": "BOOLEAN",
                                         "description": "If SnapRS devices are installed, this must be \\"on\\".",
                                         "updatedTimestampUtc": "2023-12-19T20:45:20.460199Z"
                                     },
                                     {
                                         "name": "PLM_CHANNEL",
                                         "label": "PLM Channel",
                                         "value": 8.0,
                                         "constraints": {
                                             "allowedValues": [
                                                 {
                                                     "name": "CH_0",
                                                     "value": 0.0
                                                 },
                                                 {
                                                     "name": "CH_1",
                                                     "value": 1.0
                                                 },
                                                 {
                                                     "name": "CH_2",
                                                     "value": 2.0
                                                 },
                                                 {
                                                     "name": "CH_3",
                                                     "value": 3.0
                                                 },
                                                 {
                                                     "name": "CH_4",
                                                     "value": 4.0
                                                 },
                                                 {
                                                     "name": "CH_5",
                                                     "value": 5.0
                                                 },
                                                 {
                                                     "name": "CH_6",
                                                     "value": 6.0
                                                 },
                                                 {
                                                     "name": "CH_7",
                                                     "value": 7.0
                                                 },
                                                 {
                                                     "name": "CH_8",
                                                     "value": 8.0
                                                 },
                                                 {
                                                     "name": "CH_9",
                                                     "value": 9.0
                                                 },
                                                 {
                                                     "name": "CH_10",
                                                     "value": 10.0
                                                 },
                                                 {
                                                     "name": "CH_11",
                                                     "value": 11.0
                                                 },
                                                 {
                                                     "name": "CH_12",
                                                     "value": 12.0
                                                 }
                                             ]
                                         },
                                         "type": "ENUM",
                                         "description": "Channel for REbus communications. All devices in a system must use the same channel (except REbus Beacon).",
                                         "updatedTimestampUtc": "2023-12-19T20:45:46.287722Z"
                                     },
                                     {
                                         "name": "OVERRIDE_PVRSS",
                                         "label": "Override PVRSS",
                                         "value": 1.0,
                                         "constraints": {
                                             "allowedValues": [
                                                 {
                                                     "name": "OFF",
                                                     "value": 1.0
                                                 },
                                                 {
                                                     "name": "ON",
                                                     "value": 2.0
                                                 }
                                             ]
                                         },
                                         "type": "ENUM",
                                         "description": "Closes SnapRS device without requiring a count. Should not be left ‘on’, only for troubleshooting purposes",
                                         "updatedTimestampUtc": "2023-12-05T19:51:14.410447Z"
                                     }
                                 ]
                             }
                         ]
            }
            """;
}
