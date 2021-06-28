/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.utilities;

/**
 *
 * @author tobster
 */
public class LaunchBannedApp
{
    private String strName;
    private String strSignature;
    private String strDescription;
    
    public LaunchBannedApp(String strName, String strSignature, String strDescription)
    {
        this.strName = strName;
        this.strSignature = strSignature.toLowerCase();
        this.strDescription = strDescription;
    }
    
    public String GetName() { return strName; }
    public String GetSignature() { return strSignature; }
    public String GetDescription() { return strDescription; }
    
    public boolean Matches(String strProcessSignature)
    {
        return strProcessSignature.toLowerCase().contains(strSignature);
    }
}
