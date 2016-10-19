package com.fnklabs.td.ant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.BufferUtils;
import org.usb4java.DeviceHandle;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

public class UsbDevice implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(UsbDevice.class);
    private static final int TIMEOUT = 500;

    private final DeviceHandle deviceHandle;
    private final byte inEndPoint;
    private final byte outEndPoint;
    private final int maxPacketSize;

    private UsbDevice(DeviceHandle deviceHandle, byte outEndPoint, byte inEndPoint, int maxPacketSize) {
        this.deviceHandle = deviceHandle;
        this.inEndPoint = inEndPoint;
        this.outEndPoint = outEndPoint;
        this.maxPacketSize = maxPacketSize;
    }

    static Builder forDevice(DeviceHandle deviceHandle) {
        return new Builder(deviceHandle);
    }

    public ByteBuffer read() {
        ByteBuffer data = read(deviceHandle, inEndPoint, maxPacketSize);
        data.rewind();
        return data;
    }

    public void write(byte[] data) {
        write(deviceHandle, outEndPoint, data);
    }

    @Override
    public void close() throws IOException {
        LOGGER.debug("Close device {}", deviceHandle);
        LibUsb.close(deviceHandle);
    }

    /**
     * Reads some data from the device.
     *
     * @param handle The device handle.
     * @param size   The number of bytes to read from the device.
     *
     * @return The read data.
     */
    private static ByteBuffer read(DeviceHandle handle, byte inEndPoint, int size) {
        ByteBuffer buffer = BufferUtils.allocateByteBuffer(size)
                                       .order(ByteOrder.LITTLE_ENDIAN);

        IntBuffer transferred = BufferUtils.allocateIntBuffer();

        int result = LibUsb.bulkTransfer(handle, inEndPoint, buffer, transferred, TIMEOUT);

        if (result != LibUsb.SUCCESS) {
            throw new LibUsbException("Unable to read data", result);
        }

        int retrievedBytes = transferred.get();

        LOGGER.debug("{} bytes read from device", retrievedBytes);

        return (ByteBuffer) buffer.limit(retrievedBytes);
    }

    /**
     * Writes some data to the device.
     *
     * @param handle The device handle.
     * @param data   The data to send to the device.
     */
    private static void write(DeviceHandle handle, byte outEndPoint, byte[] data) {
        ByteBuffer buffer = BufferUtils.allocateByteBuffer(data.length);
        buffer.put(data);
        IntBuffer transferred = BufferUtils.allocateIntBuffer();

        int result = LibUsb.bulkTransfer(handle, outEndPoint, buffer, transferred, TIMEOUT);

        if (result != LibUsb.SUCCESS) {
            throw new LibUsbException("Unable to send data", result);
        }

        LOGGER.debug("{} bytes sent to device", transferred.get());
    }

    static class Builder {
        private DeviceHandle deviceHandle;
        private byte inEndPoint;
        private byte outEndPoint;
        private int maxPacketSize;

        private Builder(DeviceHandle deviceHandle) {
            this.deviceHandle = deviceHandle;
        }

        Builder withInEndPoint(byte inEndPoint) {
            this.inEndPoint = inEndPoint;
            return this;
        }

        Builder withOutEndPoint(byte outEndPoint) {
            this.outEndPoint = outEndPoint;
            return this;
        }

        Builder withMaxPacketSize(int maxPacketSize) {
            this.maxPacketSize = maxPacketSize;
            return this;
        }

        UsbDevice build() {
            LOGGER.debug(
                    "Build usb device. DeviceHandle: {} Out Endpoint: 0x{} In EndPoint: 0x{} Packet Size: {}",
                    deviceHandle,
                    Integer.toHexString(Byte.toUnsignedInt(outEndPoint)),
                    Integer.toHexString(Byte.toUnsignedInt(inEndPoint)),
                    maxPacketSize
            );

            return new UsbDevice(deviceHandle, outEndPoint, inEndPoint, maxPacketSize);
        }
    }
}
