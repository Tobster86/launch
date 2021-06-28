package com.apps.fast.launch.launchviews.controls;

/**
 * Created by tobster on 23/01/17.
 */

public interface SlotListener
{
    void SlotClicked(byte cSlotNumber);
    void SlotLongClicked(byte cSlotNumber);
    boolean GetSlotOccupied(byte cSlotNumber);
    String GetSlotContents(byte cSlotNumber);
    boolean GetOnline();
    long GetSlotPrepTime(byte cSlotNumber);
    SlotControl.ImageType GetImageType(byte cSlotNumber);
}
