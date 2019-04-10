package com.example.root.maglock;

import android.os.ParcelUuid;

import java.util.UUID;

public class Constants {

    /**
     * UUID identified with this app - set as Service UUID for BLE Advertisements.
     *
     * Bluetooth requires a certain format for UUIDs associated with Services.
     * The official specification can be found here:
     * {@link https://www.bluetooth.org/en-us/specification/assigned-numbers/service-discovery}
     */
    public static final ParcelUuid Service_UUID = ParcelUuid
            .fromString("0000b81d-0000-1000-8000-00805f9b34fb");

    public static final ParcelUuid magLock_UUID = new ParcelUuid(convertFromInteger(0x3D22));
    public static final ParcelUuid magLock_UUID2 = new ParcelUuid(convertFromInteger(0x180B));

    public static final ParcelUuid door_UUID = new ParcelUuid(UUID.fromString("3d22744e-38df-4a2d-bb2e-80f582f78784"));

    public static ParcelUuid parcelUuidMask = new ParcelUuid(UUID.fromString("0000FFFF-0000-0000-0000-000000000000"));

    public static final int REQUEST_ENABLE_BT = 1;
    public static final int DEVICE_SCAN = 2;

    public static UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }
}