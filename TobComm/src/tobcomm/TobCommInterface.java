/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tobcomm;

/**
 *
 * @author tobster
 */
public interface TobCommInterface
{
    public void BytesToSend(byte cData[]);
    
    public void ObjectReceived(int lObject, int lInstanceNumber, int lOffset, byte cData[]);
    
    public void CommandReceived(int lCommand, int lInstanceNumber);
    
    public void ObjectRequested(int lObject, int lInstanceNumber, int lOffset, int lLength);
    
    public void Error(String strErrorText);
    
    public void SyncObjectsProcessed();
}
