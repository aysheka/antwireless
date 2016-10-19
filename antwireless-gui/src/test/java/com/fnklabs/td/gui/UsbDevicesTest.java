package com.fnklabs.td.gui;

import com.fnklabs.td.ant.UsbDevice;
import com.fnklabs.td.ant.UsbDevices;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class UsbDevicesTest {
    @Test
    public void write() throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(5);
        byte[] data = {
                (byte) 0xA4,
                1,
                0x4B,
                0x1,
//                (byte) 0xee
                (byte) 0xef
        };
        byteBuffer.put(data);

        List<UsbDevice> antDevice = UsbDevices.INSTANCE.findAntDevices();

        Assert.assertNotNull(antDevice);

        for (UsbDevice usbDevice : antDevice) {
            usbDevice.write(data);

            ByteBuffer read = usbDevice.read();

            List<String> arr = new ArrayList<>();

            for (int i = 0; i < read.limit(); i++) {
                String hex = String.format("0x%s", Integer.toHexString(Byte.toUnsignedInt(read.get(i))));
                arr.add(hex);
            }


            LoggerFactory.getLogger(getClass()).debug("Result: {}", arr);
        }


    }

    public void read() {

    }
}