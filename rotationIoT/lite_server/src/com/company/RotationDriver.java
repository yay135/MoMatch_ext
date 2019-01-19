package com.company;

import javax.security.auth.login.Configuration;
import javax.usb.*;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

public class RotationDriver {

    public static void getRotationData() {
	// write your code here
        try {
            //init virtual usb hub
            usbDriver driver = new usbDriver();
            final UsbServices services = UsbHostManager.getUsbServices();
            UsbHub hub = services.getRootUsbHub();
            // find desired usb device
            short vendorId = 9025;
            short productId = 18509;
            UsbDevice device = driver.findDevice(hub,vendorId,productId);
            // reads the current configuration number from a device by using a control request
            UsbControlIrp irp = device.createUsbControlIrp(
                    (byte) (UsbConst.REQUESTTYPE_DIRECTION_IN
                            | UsbConst.REQUESTTYPE_TYPE_STANDARD
                            | UsbConst.REQUESTTYPE_RECIPIENT_DEVICE),
                    UsbConst.REQUEST_GET_CONFIGURATION,
                    (short) 0,
                    (short) 0
            );
            irp.setData(new byte[1]);
            device.syncSubmit(irp);
            System.out.println(Arrays.toString(irp.getData()));
            // Use interface to communicate with devices
            UsbConfiguration configuration = device.getActiveUsbConfiguration();
            UsbInterface iface = configuration.getUsbInterface((byte) 02);
            System.out.println(iface);
            iface.claim(new UsbInterfacePolicy()
            {
                @Override
                public boolean forceClaim(UsbInterface usbInterface)
                {
                    return true;
                }
            });
            try
            {
                boolean exit = false;
                UsbEndpoint endpoint = iface.getUsbEndpoint((byte) 0x84);
                UsbPipe pipe = endpoint.getUsbPipe();
                while(!exit) {
                    try {
                        Thread.sleep(1);
                        pipe.open();
                        byte[] data = new byte[16];
                        pipe.syncSubmit(data);

                    }catch (Exception e){
                        e.printStackTrace();
                        exit = true;
                    }
                    finally {
                        pipe.close();
                    }
                }
            }
            finally
            {
                iface.release();
            }
        }catch(UsbException e){
            e.printStackTrace();
        }
    }
}

class usbDriver{
    public UsbDevice findDevice(UsbHub hub, short vendorId, short productId)
    {
        for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices())
        {
            UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
            System.out.println("Devices found:");
            System.out.println("vendorId:"+desc.idVendor()+",prodcutId:"+desc.idProduct());
            if (desc.idVendor() == vendorId && desc.idProduct() == productId) return device;
            if (device.isUsbHub())
            {
                device = findDevice((UsbHub) device, vendorId, productId);
                if (device != null) return device;
            }
        }
        System.out.println("Device not found");
        return null;
    }
}