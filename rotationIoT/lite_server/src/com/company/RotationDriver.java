package com.company;

import javax.usb.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RotationDriver {
    private boolean exit = false;
    private ArrayList<String[]> rotationData = new ArrayList<>();
    private long offSet = 0;
    private UsbInterface iface = null;
    private UsbPipe pipe = null;
    public RotationDriver(short vid, short pid){
        try {
            //init virtual usb hub
            usbDriver driver = new usbDriver();
            final UsbServices services = UsbHostManager.getUsbServices();
            UsbHub hub = services.getRootUsbHub();
            // find desired usb device
            short vendorId = vid;
            short productId = pid;
            UsbDevice device = driver.findDevice(hub, vendorId, productId);
            // reads the current configuration number from a device by using a control request
//            UsbControlIrp irp = device.createUsbControlIrp(
//                    (byte) (UsbConst.REQUESTTYPE_DIRECTION_IN
//                            | UsbConst.REQUESTTYPE_TYPE_STANDARD
//                            | UsbConst.REQUESTTYPE_RECIPIENT_DEVICE),
//                    UsbConst.REQUEST_GET_CONFIGURATION,
//                    (short) 0,
//                    (short) 0
//            );
//            irp.setData(new byte[1]);
//            device.syncSubmit(irp);
//            System.out.println(Arrays.toString(irp.getData()));
            // Use interface to communicate with devices
            UsbConfiguration configuration = device.getActiveUsbConfiguration();
            iface = configuration.getUsbInterface((byte) 02);
            iface.claim(new UsbInterfacePolicy() {
                @Override
                public boolean forceClaim(UsbInterface usbInterface) {
                    return true;
                }
            });
            UsbEndpoint endpoint = iface.getUsbEndpoint((byte) 0x84);
            pipe = endpoint.getUsbPipe();
            pipe.open();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void startRotation() {
        exit = false;
        rotationData.clear();
        byte[] data = new byte[16];
        while (!exit) {
            try {
                pipe.syncSubmit(data);
                String[] res = new String[2];
                res[0] = String.valueOf(System.currentTimeMillis() + offSet);
                res[1] = String.valueOf(data[1]);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(!res[1].equals("0")) System.out.println(res[1].equals("-23")?"right":"left");
                    }
                }).start();
                this.rotationData.add(res);
            } catch (Exception e) {
                e.printStackTrace();
                exit = true;
            }
        }
    }
    public void stopRotation(){
        this.exit = true;
    }
    public ArrayList<String[]> getRotationData() {
        return new ArrayList<>(rotationData);
    }
    public void updateOffSet(long offSet){
        this.offSet = offSet;
    }
    public void releaseAndClosePipe(){
        while(this.pipe.isActive()){
            System.out.println("pipe and interface are still busy ...");
            try{
                Thread.sleep(1000);
            }catch (Exception e){}
        }
        try {
            this.iface.release();
            this.pipe.close();
            System.out.println("pipe and interface closed.");
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("pipe and interface not closed!c");
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