package com.company;

import java.util.concurrent.CopyOnWriteArrayList;

import javax.management.Descriptor;
import javax.usb.*;
import java.util.ArrayList;
import java.util.List;

public class RotationDriver {
    private boolean exit = false;
    private List<String[]> rotationData = new CopyOnWriteArrayList<>();
    private long offSet = 0;
    private UsbInterface iface = null;
    private UsbPipe pipe = null;
    public RotationDriver(int portNumber, short vid, short pid){
        try {
            //init virtual usb hub
            usbDriver driver = new usbDriver();
            final UsbServices services = UsbHostManager.getUsbServices();
            UsbHub hub = services.getRootUsbHub();
            // find desired usb device
            short vendorId = vid;
            short productId = pid;
            byte pn = (byte)portNumber;
            UsbDevice device = driver.findDevice(pn, hub, vendorId, productId);
            //UsbDeviceDescriptor descriptor = device.getUsbDeviceDescriptor();
            //driver.getirp(device);
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
                        if(!res[1].equals("0")&&!exit) System.out.println(res[1].equals("-23")?"RIGHT":"LEFT");
                    }
                }).start();
                if(!exit) this.rotationData.add(res);
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
            int count = 0;
            if(count<5) {
                System.out.println("Pipe and interface are still busy ...");
                try {
                    count++;
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
        }
        try {
            this.iface.release();
            this.pipe.close();
            System.out.println("Pipe and interface closed.");
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Pipe and interface not closed!");
        }
    }
}

class usbDriver{
    public  byte[] getirp(UsbDevice device){
         //reads the current configuration number from a device by using a control request
        try {
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
            return irp.getData();
        }catch (UsbException e){
            e.printStackTrace();
        }
        return null;
    }
    public UsbDevice findDevice(byte portNumber, UsbHub hub, short vendorId, short productId)
    {
        for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices())
        {
            UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
            System.out.println("Devices found:");
            System.out.println("vendorId:"+desc.idVendor()+",prodcutId:"+desc.idProduct());
            if (desc.idVendor() == vendorId && desc.idProduct() == productId
                    && device.getParentUsbPort().getPortNumber()==portNumber){
                System.out.println("in port: "+portNumber);
                return device;
            }
            if (device.isUsbHub())
            {
                device = findDevice(portNumber, (UsbHub) device, vendorId, productId);
                if (device != null) return device;
            }
        }
        System.out.println("Device not found");
        return null;
    }

}