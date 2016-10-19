package com.fnklabs.td.ant;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.*;

import java.util.ArrayList;
import java.util.List;

public class UsbDevices {
    public static final UsbDevices INSTANCE = new UsbDevices();
    private static final Logger LOGGER = LoggerFactory.getLogger(UsbDevices.class);

    private final List<UsbDevice> usbDeviceList = new ArrayList<>();

    private final Context context;

    private UsbDevices() {
        context = new Context();
        // Initialize the libusb context
        int result = LibUsb.init(context);

        if (result < 0) {
            throw new LibUsbException("Unable to initialize libusb", result);
        }


    }


    public List<UsbDevice> findAntDevices() {
        ArrayList<UsbDevice> usbDevices = new ArrayList<>();

        // Read the USB device list
        DeviceList list = new DeviceList();
        int result = LibUsb.getDeviceList(context, list);

        if (result < 0) {
            throw new LibUsbException("Unable to get device list", result);
        }

        try {
            // Iterate over all devices and list them
            for (Device device : list) {
                DeviceDescriptor descriptor = new DeviceDescriptor();
                result = LibUsb.getDeviceDescriptor(device, descriptor);

                if (result < 0) {
                    throw new LibUsbException("Unable to read device descriptor", result);
                }


                if (descriptor.idVendor() == AntHub.ANT_PLUS_VENDOR_ID) {
//                    dumpDevice(device);

                    DeviceHandle deviceHandle = new DeviceHandle();

                    result = LibUsb.open(device, deviceHandle);

                    if (result != LibUsb.SUCCESS) {
                        throw new LibUsbException("Unable to open device", result);
                    }


                    result = LibUsb.claimInterface(deviceHandle, 0);
                    if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to claim interface", result);


                    UsbDevice.Builder builder = UsbDevice.forDevice(deviceHandle)
                                                         .withMaxPacketSize(Byte.valueOf(descriptor.bMaxPacketSize0()).intValue());


                    // Dump all configuration descriptors
                    final byte numConfigurations = descriptor.bNumConfigurations();

                    for (byte i = 0; i < numConfigurations; i += 1) {
                        final ConfigDescriptor configurationDescriptor = new ConfigDescriptor();

                        final int configurationResult = LibUsb.getConfigDescriptor(device, i, configurationDescriptor);

                        if (configurationResult < 0) {
                            throw new LibUsbException("Unable to read config configurationDescriptor", configurationResult);
                        }

                        for (Interface iface : configurationDescriptor.iface()) {
                            for (InterfaceDescriptor interfaceDescriptor : iface.altsetting()) {
                                LOGGER.debug("{}", DescriptorUtils.dump(interfaceDescriptor));

                                for (EndpointDescriptor endpointDescriptor : interfaceDescriptor.endpoint()) {
                                    LOGGER.debug("{}", DescriptorUtils.dump(endpointDescriptor));

                                    String directionName = DescriptorUtils.getDirectionName(endpointDescriptor.bEndpointAddress());
                                    byte endPoint = endpointDescriptor.bEndpointAddress();


                                    LOGGER.debug("Endpoint: {} {}", Integer.toHexString(Byte.toUnsignedInt(endPoint)), directionName);

                                    if (StringUtils.equals("IN", directionName)) {
                                        builder.withInEndPoint(endPoint);
                                    } else if (StringUtils.equals("OUT", directionName)) {
                                        builder.withOutEndPoint(endPoint);
                                    }
                                }
                            }
                        }

                        LibUsb.freeConfigDescriptor(configurationDescriptor);

                    }

                    usbDevices.add(builder.build());
                }
            }
        } finally {
            // Ensure the allocated device list is freed
            LibUsb.freeDeviceList(list, true);
        }

        return usbDevices;
    }

    @Override
    protected void finalize() throws Throwable {
        LibUsb.exit(context);

        for (UsbDevice usbDevice : usbDeviceList) {
            try {
                usbDevice.close();
            } catch (Exception e) {

            }
        }
    }
}
