package com.fnklabs.td.ant;

import com.google.common.base.MoreObjects;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Message {
    public static final byte CAN_WRITE = (byte) 0xA4;
    public static final byte CAN_READ = (byte) 0xA5;

    public static final byte ANT_OPEN_CHANNEL = (byte)0x4B;

    private final byte sync;
    private final byte id;
    private final byte[] data;

    public byte getSync() {
        return sync;
    }

    public byte getId() {
        return id;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("sync", String.format("0x%s", Integer.toHexString(Byte.toUnsignedInt(getSync()))))
                          .add("id", String.format("0x%s", Integer.toHexString(Byte.toUnsignedInt(getId()))))
                          .add("data", pack(getData()))
                          .toString();
    }

    public Message(byte sync, byte id, byte[] data) {
        this.sync = sync;
        this.id = id;
        this.data = data;
    }

    public static byte[] pack(byte sync, byte id, byte length, byte[] data) {
        ByteBuffer msgData = ByteBuffer.allocate(3 + length + 1);

        msgData.put(sync);
        msgData.put(id);
        msgData.put(length);
        msgData.put(data);

        int checksum = getChecksum(sync, id, length, data);

        msgData.put((byte) checksum);

        return msgData.array();
    }

    public static List<String> pack(byte[] data) {
        List<String> str = new ArrayList<>();

        for (int i = 0; i < data.length; i++) {
            str.add("0x" + Integer.toHexString(Byte.toUnsignedInt(data[i])));
        }

        return str;
    }

    public static List<String> pack(ByteBuffer data) {
        List<String> str = new ArrayList<>();

        for (int i = 0; i < data.limit(); i++) {
            str.add("0x" + Integer.toHexString(Byte.toUnsignedInt(data.get(i))));
        }

        return str;
    }

    public static Message unpack(ByteBuffer read) {
        byte[] data = new byte[read.limit()];

        for (int i = 0; i < read.limit(); i++) {
            data[i] = read.get(i);
        }

        return unpack(data);
    }

    public static Message unpack(byte[] data) {
        int length = Byte.toUnsignedInt(data[1]);

        byte[] messageData = new byte[length];

        for (int i = 0; i < length; i++) {
            messageData[i] = data[3 + i];
        }

        byte checkSum = data[3 + length];


        if (checkSum != getChecksum(data[0], data[2], data[1], messageData)) {
            throw new RuntimeException("Invalid checksum");
        }

        return new Message(data[0], data[2], messageData);
    }

    public byte[] pack() {
        return pack(sync, id, (byte) data.length, data);
    }

    private static int getChecksum(byte sync, byte id, byte length, byte[] data) {
        int checksum = sync ^ id ^ length;

        for (int i = 0; i < data.length; i++) {
            checksum ^= data[i];
        }
        return checksum;
    }
}
