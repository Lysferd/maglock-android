package com.example.root.maglock;

import java.util.HashMap;
import java.util.UUID;

public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public static UUID HEART_RATE_UUID = convertFromInteger(0x180D);
    public static UUID HEART_RATE_MEASUREMENTE_UUID = convertFromInteger(0x2A37);

    public static UUID BATTERY_SERVICE_UUID = convertFromInteger(0x180F);
    public static UUID BATTERY_LEVEL_UUID = convertFromInteger(0x2A19);

    public static UUID GENERIC_ACCESS_UUID = convertFromInteger(0x1800);
    public static UUID DEVICE_NAME_UUID = convertFromInteger(0x2A00);
    public static UUID APPEARANCE_UUID = convertFromInteger(0x2A01);

    public static UUID GENERIC_ATTRIBUTE_UUID = convertFromInteger(0x1801);
    public static UUID SERVICE_CHANGE_UUID = convertFromInteger(0x2A05);

    public static UUID RANDOM_SERVICE_UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0");
    public static UUID RANDOM_CHARACTERISTIC_UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef1");

    public static UUID TEST_DESCRIPTOR_UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef2");

    public static UUID DOOR_SERVICE_UUID = UUID.fromString("3d22744e-38df-4a2d-bb2e-80f582f78784");
    public static UUID DOOR_CONTACT_CHARACTERISTIC = UUID.fromString("e53a7a22-0850-48e5-9f25-5864e71eb00a");
    public static UUID DOOR_STRIKE_CHARACTERISTIC = UUID.fromString("a1fd909e-b168-452a-99fe-621db9c0111a");
    public static UUID DOOR_REQ_CHARACTERISTIC = UUID.fromString("8f3625e6-5f63-4bf8-872b-8786a911b620");

    public static UUID DEVICE_INFORMATION_SERVICE_UUID = convertFromInteger(0x180A);
    public static UUID SERIAL_NUMBER_CHARACTERISTIC_UUID = convertFromInteger(0x2A25);
    public static UUID SERIAL_NUMBER_DESCRIPTOR_UUID = convertFromInteger(0x2901);

    static {
        // Sample Services.
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        // Sample Characteristics.
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
        // Battery Service
        attributes.put(String.valueOf(BATTERY_SERVICE_UUID), "Battery Service");
        attributes.put(String.valueOf(BATTERY_LEVEL_UUID), "Battery Level");
        // Generic Access
        attributes.put(String.valueOf(GENERIC_ACCESS_UUID), "Generic Access Service");
        attributes.put(String.valueOf(DEVICE_NAME_UUID), "Device Name");
        attributes.put(String.valueOf(APPEARANCE_UUID), "Appearance");
        // Random Test Service
        //attributes.put("12345678-1234-5678-1234-56789abcdef0", "Random Test Service");
        attributes.put(String.valueOf(RANDOM_SERVICE_UUID), "Random Test Service");
        // Random Test Characteristics
        attributes.put("12345678-1234-5678-1234-56789abcdef5", "Randomm Test characteristic 1");
        attributes.put("12345678-1234-5678-1234-56789abcdef3", "Randomm Test characteristic 2");
        //attributes.put("12345678-1234-5678-1234-56789abcdef1", "Randomm Test characteristic 3");
        attributes.put(String.valueOf(RANDOM_CHARACTERISTIC_UUID), "Random Characteristic");
        attributes.put(String.valueOf(TEST_DESCRIPTOR_UUID), "Test Descriptor");
        // Generic Attribute Service
        attributes.put(String.valueOf(GENERIC_ATTRIBUTE_UUID), "Generic Attribute Service");
        // Generic Attribute Service - Service Changed Characteristic
        attributes.put(String.valueOf(SERVICE_CHANGE_UUID), "Service Changed");
        // Heart Rate Service
        attributes.put(String.valueOf(HEART_RATE_UUID), "Heart Rate Service");
        attributes.put(String.valueOf(HEART_RATE_MEASUREMENTE_UUID), "Heart Rate Measurement");
        // Door Service
        attributes.put(String.valueOf(DOOR_SERVICE_UUID), "Door Service");
        attributes.put(String.valueOf(DOOR_CONTACT_CHARACTERISTIC), "Door Contact Characteristic");
        attributes.put(String.valueOf(DOOR_STRIKE_CHARACTERISTIC), "Door Strike Characteristic");
        attributes.put(String.valueOf(DOOR_REQ_CHARACTERISTIC), "Door Requisition Characteristic");
        // Device information
        attributes.put(String.valueOf(DEVICE_INFORMATION_SERVICE_UUID), "Device Information Service");
        attributes.put(String.valueOf(SERIAL_NUMBER_CHARACTERISTIC_UUID), "Serial Number Characteristic");
        attributes.put(String.valueOf(SERIAL_NUMBER_DESCRIPTOR_UUID), "Serial Number Descriptor");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

    public static Boolean match(String uuid, UUID mUUID) {
        if (String.valueOf(mUUID).equals(uuid)) {
            return true;
        }
        return false;
    }

    public static UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }
}
