package com.fnklabs.td.ant;

import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class AntHub {
    static final int ANT_PLUS_VENDOR_ID = 0x0FCF;

    private final UsbDevice usbDevice;

    AntHub(UsbDevice usbDevice) {
        this.usbDevice = usbDevice;
    }

    public Message send(Message message) {
        byte[] data = message.pack();

        LoggerFactory.getLogger(getClass()).debug("Send message: {}", Message.pack(data));

        usbDevice.write(data);

        ByteBuffer read = usbDevice.read();

        LoggerFactory.getLogger(getClass()).debug("Receive message: {}", Message.pack(read));

        return Message.unpack(read);
    }
}
