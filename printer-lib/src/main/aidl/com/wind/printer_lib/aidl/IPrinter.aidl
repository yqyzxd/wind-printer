// IPrinter.aidl
package com.wind.printer_lib.aidl;
import com.wind.printer_lib.aidl.EscCommand;
import com.wind.printer_lib.aidl.LabelCommand;
// Declare any non-default types here with import statements

interface IPrinter {
   int openDevice(String deviceName,int connectMode);
   int sendEscCommand(String deviceName,in EscCommand command);
   int sendLabelCommand(String deviceName,in LabelCommand command);
   void closeDevice(String deviceName);
   int getDeviceState(String deviceName);
}
