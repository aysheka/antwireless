package com.fnklabs.td.ant;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AntHubTest {

    @Test
    public void testMessage() throws Exception {
        List<UsbDevice> antDevices = UsbDevices.INSTANCE.findAntDevices();

        Assert.assertFalse(antDevices.isEmpty());

        UsbDevice usbDevice = antDevices.get(0);

        AntHub antHub = new AntHub(usbDevice);

        Message response = antHub.send(new Message(
                Message.CAN_READ,
                Message.ANT_OPEN_CHANNEL,
                new byte[]{0x1}
        ));


        LoggerFactory.getLogger(getClass()).debug("Response: {}", response);
    }
}