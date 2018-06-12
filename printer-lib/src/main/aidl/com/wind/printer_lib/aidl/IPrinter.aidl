// IPrinter.aidl
package com.wind.printer_lib.aidl;
import com.wind.printer_lib.aidl.EscCommand;
// Declare any non-default types here with import statements

interface IPrinter {
   int openDevice(String deviceName,int connectMode);
   int sendEscCommand(String deviceName,in EscCommand command);

   void closeDevice(String deviceName);
}
