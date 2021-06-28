/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package launch.game.entities;

import launch.game.Alliance;

/**
 *
 * @author tobster
 */
public interface LaunchEntityListener
{
    void EntityChanged(LaunchEntity entity, boolean bOwner);
    void EntityChanged(Alliance alliance);
}
